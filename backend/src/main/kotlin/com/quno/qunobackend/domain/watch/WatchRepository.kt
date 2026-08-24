package com.quno.qunobackend.domain.watch

/**
 * Port for the watches relation table. Pure relation data (hard delete allowed) — Ward is
 * a subscription to a Question's future activity, not a bookmark; see docs/product/vision.md.
 */
interface WatchRepository {
    /** Idempotent. */
    fun watch(userId: Long, questionId: Long)

    /** Idempotent. */
    fun unwatch(userId: Long, questionId: Long)
    fun isWatching(userId: Long, questionId: Long): Boolean
    fun findWatchedQuestionIds(userId: Long): List<Long>

    /** Used by the Notification fan-out (Phase 2.8) to find who to notify. */
    fun findWatcherIds(questionId: Long): List<Long>
}
