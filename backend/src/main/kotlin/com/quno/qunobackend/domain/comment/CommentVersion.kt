package com.quno.qunobackend.domain.comment

import java.time.Instant

/** Immutable, append-only history entry for a Comment (Cv1, Cv2, ...) — mirrors AnswerVersion,
 * but without a diff use case (comments are at most 600 chars, a plain before/after list is
 * enough — see ADR-0031 #2). Never updated after creation. */
class CommentVersion private constructor(
    val id: Long?,
    val commentId: Long,
    val versionNumber: Int,
    val body: String,
    val createdAt: Instant,
) {
    companion object {
        fun create(commentId: Long, versionNumber: Int, body: String): CommentVersion {
            require(versionNumber >= 1) { "versionNumber must be >= 1" }
            require(body.isNotBlank()) { "body must not be blank" }
            return CommentVersion(id = null, commentId = commentId, versionNumber = versionNumber, body = body, createdAt = Instant.now())
        }

        fun reconstitute(id: Long, commentId: Long, versionNumber: Int, body: String, createdAt: Instant) =
            CommentVersion(id, commentId, versionNumber, body, createdAt)
    }
}
