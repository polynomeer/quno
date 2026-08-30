package com.quno.qunobackend.application.comment.dto

import com.quno.qunobackend.domain.comment.CommentTargetType
import java.time.Instant

data class CommentResult(
    val id: Long,
    val targetType: CommentTargetType,
    val targetId: Long,
    val authorId: Long,
    /** Null once deleted — the tombstone hides the original text even in this internal result,
     * not just the API response (see ADR-0024 #4). */
    val body: String?,
    val isDeleted: Boolean,
    val createdAt: Instant,
    val updatedAt: Instant,
)
