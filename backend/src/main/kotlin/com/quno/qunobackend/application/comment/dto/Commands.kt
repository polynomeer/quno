package com.quno.qunobackend.application.comment.dto

import com.quno.qunobackend.domain.comment.CommentTargetType

data class CreateCommentCommand(
    val targetType: CommentTargetType,
    val targetId: Long,
    val authorId: Long,
    val body: String,
    val parentCommentId: Long? = null,
)

data class EditCommentCommand(
    val commentId: Long,
    val editorId: Long,
    val body: String,
)
