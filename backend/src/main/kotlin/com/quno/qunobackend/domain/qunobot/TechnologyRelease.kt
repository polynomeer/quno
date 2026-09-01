package com.quno.qunobackend.domain.qunobot

import java.time.Instant
import java.time.LocalDate

/**
 * Last-known latest release for one tracked technology tag (Phase 21, ADR-0033) — a snapshot
 * used purely for change detection, not a history log. [updatedAt] only moves when
 * [latestVersion] actually changes between scans; [checkedAt] moves on every scan regardless.
 */
data class TechnologyRelease(
    val id: Long?,
    val tagSlug: String,
    val productSlug: String,
    val latestVersion: String,
    val latestReleaseDate: LocalDate,
    val checkedAt: Instant,
    val updatedAt: Instant,
) {
    /** Applies a fresh scan result. Bumps [updatedAt] only when the version actually changed. */
    fun recheck(latestVersion: String, latestReleaseDate: LocalDate): TechnologyRelease {
        val now = Instant.now()
        val changed = latestVersion != this.latestVersion
        return copy(
            latestVersion = latestVersion,
            latestReleaseDate = latestReleaseDate,
            checkedAt = now,
            updatedAt = if (changed) now else updatedAt,
        )
    }

    companion object {
        /** First time a tag is seen — seeded as a baseline, never treated as "a new release". */
        fun seed(tagSlug: String, productSlug: String, latestVersion: String, latestReleaseDate: LocalDate): TechnologyRelease {
            val now = Instant.now()
            return TechnologyRelease(
                id = null,
                tagSlug = tagSlug,
                productSlug = productSlug,
                latestVersion = latestVersion,
                latestReleaseDate = latestReleaseDate,
                checkedAt = now,
                updatedAt = now,
            )
        }
    }
}
