package com.quno.qunobackend.interfaces.api.comment

import com.quno.qunobackend.application.comment.dto.CommentResult
import com.quno.qunobackend.application.comment.dto.CommentVersionResult
import com.quno.qunobackend.domain.comment.CommentTargetType
import java.time.Instant

data class CommentResponse(
    val id: Long,
    val targetType: CommentTargetType,
    val targetId: Long,
    val authorId: Long,
    val parentCommentId: Long?,
    /** Null once deleted — see ADR-0024 #4 (the original text isn't kept in any response, tombstoned). */
    val body: String?,
    val versionNumber: Int,
    val isDeleted: Boolean,
    val createdAt: Instant,
    val updatedAt: Instant,
)

fun CommentResult.toResponse() = CommentResponse(
    id = id,
    targetType = targetType,
    targetId = targetId,
    authorId = authorId,
    parentCommentId = parentCommentId,
    body = body,
    versionNumber = versionNumber,
    isDeleted = isDeleted,
    createdAt = createdAt,
    updatedAt = updatedAt,
)

data class CommentVersionResponse(
    val versionNumber: Int,
    val body: String,
    val createdAt: Instant,
)

fun CommentVersionResult.toResponse() = CommentVersionResponse(
    versionNumber = versionNumber,
    body = body,
    createdAt = createdAt,
)
