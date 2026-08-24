package com.quno.qunobackend.domain.tag

/** Port for the user_tag_follows relation table (hard delete allowed). */
interface UserTagFollowRepository {
    /** Idempotent. */
    fun follow(userId: Long, tagId: Long)

    /** Idempotent. */
    fun unfollow(userId: Long, tagId: Long)
    fun isFollowing(userId: Long, tagId: Long): Boolean
    fun findFollowedTagIds(userId: Long): List<Long>
}
