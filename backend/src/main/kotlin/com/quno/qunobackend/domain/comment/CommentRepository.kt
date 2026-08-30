package com.quno.qunobackend.domain.comment

interface CommentRepository {
    fun save(comment: Comment): Comment

    /** No deletedAt filter — a soft-deleted comment is still findable so re-deleting it stays
     * idempotent and it still shows up (tombstoned) in [listByTarget]. */
    fun findById(id: Long): Comment?

    /** Newest-last (chronological) — comments are a flat discussion thread, not a ranked list. */
    fun listByTarget(targetType: CommentTargetType, targetId: Long): List<Comment>
}
