package com.quno.qunobackend.application.comment.dto

import com.quno.qunobackend.domain.comment.CommentTargetType

data class CreateCommentCommand(
    val targetType: CommentTargetType,
    val targetId: Long,
    val authorId: Long,
    val body: String,
)
