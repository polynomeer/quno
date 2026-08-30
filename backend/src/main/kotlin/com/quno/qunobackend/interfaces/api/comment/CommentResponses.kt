package com.quno.qunobackend.interfaces.api.comment

import com.quno.qunobackend.application.comment.dto.CommentResult
import com.quno.qunobackend.domain.comment.CommentTargetType
import java.time.Instant

data class CommentResponse(
    val id: Long,
    val targetType: CommentTargetType,
    val targetId: Long,
    val authorId: Long,
    /** Null once deleted — see ADR-0024 #4 (the original text isn't kept in any response, tombstoned). */
    val body: String?,
    val isDeleted: Boolean,
    val createdAt: Instant,
    val updatedAt: Instant,
)

fun CommentResult.toResponse() = CommentResponse(
    id = id,
    targetType = targetType,
    targetId = targetId,
    authorId = authorId,
    body = body,
    isDeleted = isDeleted,
    createdAt = createdAt,
    updatedAt = updatedAt,
)
