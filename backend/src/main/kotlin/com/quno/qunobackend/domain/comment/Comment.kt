package com.quno.qunobackend.domain.comment

import java.time.Instant

/** Stack Overflow uses the same limit — keeps a comment a short clarification, not a second answer
 * (see docs/architecture/decisions/0024-comment-flat-no-edit-tombstone-delete.md #8). Top-level
 * (not nested in a companion) so it can also be used as a Bean Validation annotation argument. */
const val MAX_COMMENT_BODY_LENGTH = 600

/**
 * Independent side-aggregate, same pattern as Watch/Vote — Question/Answer never hold a
 * reference to this. Flat (no threading), no edit; soft-delete via [deletedAt] tombstones the
 * row instead of removing it, matching Question/Answer/Tag's own delete convention
 * (see docs/architecture/decisions/0024-comment-flat-no-edit-tombstone-delete.md).
 */
class Comment private constructor(
    val id: Long?,
    val targetType: CommentTargetType,
    val targetId: Long,
    val authorId: Long,
    val body: String,
    val deletedAt: Instant?,
    val createdAt: Instant,
    val updatedAt: Instant,
) {
    /** Idempotent — deleting an already-deleted comment is a no-op, not an error. */
    fun softDelete(): Comment {
        if (deletedAt != null) return this
        return Comment(id, targetType, targetId, authorId, body, Instant.now(), createdAt, Instant.now())
    }

    companion object {
        fun write(targetType: CommentTargetType, targetId: Long, authorId: Long, body: String): Comment {
            require(body.isNotBlank()) { "body must not be blank" }
            require(body.length <= MAX_COMMENT_BODY_LENGTH) { "body must be at most $MAX_COMMENT_BODY_LENGTH characters" }
            val now = Instant.now()
            return Comment(
                id = null,
                targetType = targetType,
                targetId = targetId,
                authorId = authorId,
                body = body,
                deletedAt = null,
                createdAt = now,
                updatedAt = now,
            )
        }

        fun reconstitute(
            id: Long,
            targetType: CommentTargetType,
            targetId: Long,
            authorId: Long,
            body: String,
            deletedAt: Instant?,
            createdAt: Instant,
            updatedAt: Instant,
        ) = Comment(id, targetType, targetId, authorId, body, deletedAt, createdAt, updatedAt)
    }
}
