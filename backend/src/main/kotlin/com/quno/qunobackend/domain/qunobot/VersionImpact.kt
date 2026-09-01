package com.quno.qunobackend.domain.qunobot

import java.time.LocalDate

/**
 * A question that may need revisiting because a tracked technology it's tagged with released a
 * new version after the question's content was last touched (Phase 21, ADR-0033). Read-only
 * reporting model, same as [TagSpike] — a signal for a human to judge, not a verdict.
 */
data class VersionImpact(
    val questionId: Long,
    val questionTitle: String,
    val tagSlug: String,
    val productSlug: String,
    val latestVersion: String,
    val latestReleaseDate: LocalDate,
)

/** The pair a notification needs — kept separate from [VersionImpact] since the notification
 * fan-out never needs the question's title. */
data class AffectedQuestion(val questionId: Long, val questionAuthorId: Long)
