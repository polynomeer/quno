package com.quno.qunobackend.domain.qunobot

import java.time.LocalDate

/** Port implemented by infrastructure/persistence/jpa/adapter/VersionImpactRepositoryAdapter. */
interface VersionImpactRepository {
    /**
     * Non-RESOLVED, non-OUTDATED questions tagged [tagSlug] whose content hasn't changed since
     * [sinceDate] — used right after detecting a version change, to fan out notifications for
     * that one tag.
     */
    fun findAffectedQuestions(tagSlug: String, sinceDate: LocalDate, limit: Int): List<AffectedQuestion>

    /** Same shape, across every tracked tag currently in technology_releases — the live view
     * behind `GET /qunobot/version-impacts`. Ranked by release date descending. */
    fun findVersionImpacts(limit: Int): List<VersionImpact>
}
