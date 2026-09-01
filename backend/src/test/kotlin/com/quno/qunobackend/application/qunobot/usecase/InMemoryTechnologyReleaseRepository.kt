package com.quno.qunobackend.application.qunobot.usecase

import com.quno.qunobackend.domain.qunobot.TechnologyRelease
import com.quno.qunobackend.domain.qunobot.TechnologyReleaseRepository

class InMemoryTechnologyReleaseRepository : TechnologyReleaseRepository {
    private val byTagSlug = mutableMapOf<String, TechnologyRelease>()
    private var nextId = 1L

    override fun findByTagSlug(tagSlug: String): TechnologyRelease? = byTagSlug[tagSlug]

    override fun save(release: TechnologyRelease): TechnologyRelease {
        val saved = if (release.id == null) release.copy(id = nextId++) else release
        byTagSlug[saved.tagSlug] = saved
        return saved
    }
}
