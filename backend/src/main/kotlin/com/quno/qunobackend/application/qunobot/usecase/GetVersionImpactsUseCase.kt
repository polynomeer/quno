package com.quno.qunobackend.application.qunobot.usecase

import com.quno.qunobackend.domain.qunobot.VersionImpact
import com.quno.qunobackend.domain.qunobot.VersionImpactRepository
import org.springframework.stereotype.Service

/** Read-only reporting model — see ADR-0010, [VersionImpact] is reused as-is through to the API layer. */
@Service
class GetVersionImpactsUseCase(
    private val versionImpactRepository: VersionImpactRepository,
) {
    fun execute(limit: Int): List<VersionImpact> = versionImpactRepository.findVersionImpacts(limit)
}
