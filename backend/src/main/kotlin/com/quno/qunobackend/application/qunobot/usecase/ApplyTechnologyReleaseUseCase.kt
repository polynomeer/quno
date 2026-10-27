package com.quno.qunobackend.application.qunobot.usecase

import com.quno.qunobackend.domain.common.OutboxEvent
import com.quno.qunobackend.domain.common.OutboxEventRepository
import com.quno.qunobackend.domain.common.OutboxEventTypes
import com.quno.qunobackend.domain.qunobot.FetchedTechnologyRelease
import com.quno.qunobackend.domain.qunobot.TechnologyRelease
import com.quno.qunobackend.domain.qunobot.TechnologyReleaseRepository
import com.quno.qunobackend.domain.qunobot.TrackedTechnologies
import com.quno.qunobackend.domain.qunobot.VersionImpactRepository
import com.quno.qunobackend.domain.stack.AnswerApplicabilityRepository
import com.quno.qunobackend.domain.stack.ApplicabilityVerdict
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * Records one product's freshly-fetched release and, only when it's genuinely a *new* version
 * since the last scan, fans out TECH_VERSION_IMPACT_DETECTED to questions tagged with it
 * (Phase 21, ADR-0033). Kept as its own transactional use case — separate from
 * [ScanTechnologyReleasesUseCase], which loops over products and makes the external HTTP calls —
 * because a single `@Transactional` method can't span both an outbound network call and the
 * eventual DB writes without holding a connection open across the network round trip.
 *
 * Deliberately does **not** mark any question OUTDATED — see ADR-0033: this only automates
 * *detection*, matching ADR-0017's judgment that state changes based on an unverified heuristic
 * (tag match + a stale-content date) are risky. A human still decides via
 * `POST /questions/{id}/outdated` after reading the notification this produces.
 *
 * Phase 63.4 (ADR-0077) narrows the fan-out: a question whose answers declare an applicability
 * range that still covers the new version is skipped, and the payload says why the rest were kept
 * (`NO_RANGE` — no range for this product, the old heuristic; `OUT_OF_RANGE`).
 */
@Service
class ApplyTechnologyReleaseUseCase(
    private val technologyReleaseRepository: TechnologyReleaseRepository,
    private val versionImpactRepository: VersionImpactRepository,
    private val outboxEventRepository: OutboxEventRepository,
    private val answerApplicabilityRepository: AnswerApplicabilityRepository? = null,
) {
    @Transactional
    fun execute(tagSlug: String, productSlug: String, fetched: FetchedTechnologyRelease) {
        val existing = technologyReleaseRepository.findByTagSlug(tagSlug)
        val updated = existing?.recheck(fetched.version, fetched.releaseDate)
            ?: TechnologyRelease.seed(tagSlug, productSlug, fetched.version, fetched.releaseDate)
        technologyReleaseRepository.save(updated)

        // First time this tag is tracked (no baseline yet) or the version genuinely didn't
        // change since last scan — nothing new to tell anyone about.
        val isNewVersion = existing != null && existing.latestVersion != fetched.version
        if (!isNewVersion) return

        val candidates = versionImpactRepository.findAffectedQuestions(tagSlug, fetched.releaseDate, limit = 100)
        val patterns = TrackedTechnologies.PACKAGES[tagSlug].orEmpty()
        val rangesByQuestionId = if (patterns.isEmpty()) {
            emptyMap()
        } else {
            answerApplicabilityRepository?.findByQuestionIds(candidates.map { it.questionId }).orEmpty()
        }
        candidates.forEach { question ->
            val verdict = ApplicabilityVerdict.of(rangesByQuestionId[question.questionId].orEmpty(), patterns, fetched.version)
            if (verdict == ApplicabilityVerdict.IN_RANGE) return@forEach
            outboxEventRepository.save(
                OutboxEvent.create(
                    eventType = OutboxEventTypes.TECH_VERSION_IMPACT_DETECTED,
                    aggregateType = "QUESTION",
                    aggregateId = question.questionId,
                    payload = """{"questionAuthorId":${question.questionAuthorId},"tagSlug":"$tagSlug","productSlug":"$productSlug","latestVersion":"${fetched.version}","latestReleaseDate":"${fetched.releaseDate}","reason":"${verdict.name}"}""",
                ),
            )
        }
    }
}
