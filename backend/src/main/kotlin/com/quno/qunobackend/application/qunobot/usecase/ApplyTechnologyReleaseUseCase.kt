package com.quno.qunobackend.application.qunobot.usecase

import com.quno.qunobackend.domain.common.OutboxEvent
import com.quno.qunobackend.domain.common.OutboxEventRepository
import com.quno.qunobackend.domain.common.OutboxEventTypes
import com.quno.qunobackend.domain.qunobot.FetchedTechnologyRelease
import com.quno.qunobackend.domain.qunobot.TechnologyRelease
import com.quno.qunobackend.domain.qunobot.TechnologyReleaseRepository
import com.quno.qunobackend.domain.qunobot.VersionImpactRepository
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
 */
@Service
class ApplyTechnologyReleaseUseCase(
    private val technologyReleaseRepository: TechnologyReleaseRepository,
    private val versionImpactRepository: VersionImpactRepository,
    private val outboxEventRepository: OutboxEventRepository,
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

        val affected = versionImpactRepository.findAffectedQuestions(tagSlug, fetched.releaseDate, limit = 100)
        affected.forEach { question ->
            outboxEventRepository.save(
                OutboxEvent.create(
                    eventType = OutboxEventTypes.TECH_VERSION_IMPACT_DETECTED,
                    aggregateType = "QUESTION",
                    aggregateId = question.questionId,
                    payload = """{"questionAuthorId":${question.questionAuthorId},"tagSlug":"$tagSlug","productSlug":"$productSlug","latestVersion":"${fetched.version}","latestReleaseDate":"${fetched.releaseDate}"}""",
                ),
            )
        }
    }
}
