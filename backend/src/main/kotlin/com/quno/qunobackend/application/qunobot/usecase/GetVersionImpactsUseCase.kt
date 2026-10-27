package com.quno.qunobackend.application.qunobot.usecase

import com.quno.qunobackend.domain.qunobot.TrackedTechnologies
import com.quno.qunobackend.domain.qunobot.VersionImpact
import com.quno.qunobackend.domain.qunobot.VersionImpactRepository
import com.quno.qunobackend.domain.stack.AnswerApplicabilityRepository
import com.quno.qunobackend.domain.stack.ApplicabilityVerdict
import org.springframework.stereotype.Service

/**
 * Read-only reporting model — see ADR-0010, [VersionImpact] is reused as-is through to the API
 * layer. Filtered by the same applicability rule as the notifications (Phase 63.4, ADR-0077), so
 * the list never shows a question nobody was told about.
 */
@Service
class GetVersionImpactsUseCase(
    private val versionImpactRepository: VersionImpactRepository,
    private val answerApplicabilityRepository: AnswerApplicabilityRepository? = null,
) {
    fun execute(limit: Int): List<VersionImpact> {
        val impacts = versionImpactRepository.findVersionImpacts(limit)
        val ranges = answerApplicabilityRepository?.findByQuestionIds(impacts.map { it.questionId }.toSet()).orEmpty()
        return impacts.filter { impact ->
            val patterns = TrackedTechnologies.PACKAGES[impact.tagSlug].orEmpty()
            ApplicabilityVerdict.of(ranges[impact.questionId].orEmpty(), patterns, impact.latestVersion) != ApplicabilityVerdict.IN_RANGE
        }
    }
}
