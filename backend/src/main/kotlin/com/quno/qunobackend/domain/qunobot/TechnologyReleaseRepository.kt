package com.quno.qunobackend.domain.qunobot

/** Port implemented by infrastructure/persistence/jpa/adapter/TechnologyReleaseRepositoryAdapter. */
interface TechnologyReleaseRepository {
    fun findByTagSlug(tagSlug: String): TechnologyRelease?
    fun save(release: TechnologyRelease): TechnologyRelease
}
