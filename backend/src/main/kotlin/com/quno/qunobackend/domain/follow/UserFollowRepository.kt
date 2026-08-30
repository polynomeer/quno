package com.quno.qunobackend.domain.follow

/**
 * Port for the user_follows relation table. Pure relation data (hard delete allowed) — records
 * "who follows whom" only, no activity feed or notification is built on top of it yet (Phase 14,
 * ADR-0026).
 */
interface UserFollowRepository {
    /** Idempotent. */
    fun follow(followerId: Long, followeeId: Long)

    /** Idempotent. */
    fun unfollow(followerId: Long, followeeId: Long)
    fun isFollowing(followerId: Long, followeeId: Long): Boolean
    fun findFolloweeIds(followerId: Long): List<Long>
}
