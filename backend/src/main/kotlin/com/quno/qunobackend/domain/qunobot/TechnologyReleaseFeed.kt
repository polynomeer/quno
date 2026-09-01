package com.quno.qunobackend.domain.qunobot

import java.time.LocalDate

/** One technology's current latest release, as reported by an external feed. */
data class FetchedTechnologyRelease(val version: String, val releaseDate: LocalDate)

/**
 * Outbound port to an external technology-release data feed (Phase 21, ADR-0033) — the piece
 * that was entirely missing before this Phase (see ADR-0017), making real automatic detection
 * possible instead of the user-only marking `POST /questions/{id}/outdated` still uses.
 * Implemented by infrastructure/external/EndOfLifeDateTechnologyReleaseFeed.
 */
interface TechnologyReleaseFeed {
    /**
     * Returns null if the product is unknown to the feed or the call fails for any reason —
     * callers must skip that product for this scan tick rather than fail the whole batch, since
     * this is the only place in the codebase that depends on a third-party service's uptime.
     */
    fun fetchLatest(productSlug: String): FetchedTechnologyRelease?
}
