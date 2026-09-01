package com.quno.qunobackend.application.qunobot.usecase

import com.quno.qunobackend.domain.qunobot.AffectedQuestion
import com.quno.qunobackend.domain.qunobot.VersionImpact
import com.quno.qunobackend.domain.qunobot.VersionImpactRepository
import java.time.LocalDate

class InMemoryVersionImpactRepository : VersionImpactRepository {
    /** Keyed by tagSlug — set directly by tests instead of computing the real join. */
    var affectedQuestionsByTag: Map<String, List<AffectedQuestion>> = emptyMap()
    var versionImpacts: List<VersionImpact> = emptyList()

    override fun findAffectedQuestions(tagSlug: String, sinceDate: LocalDate, limit: Int): List<AffectedQuestion> =
        (affectedQuestionsByTag[tagSlug] ?: emptyList()).take(limit)

    override fun findVersionImpacts(limit: Int): List<VersionImpact> = versionImpacts.take(limit)
}
