package com.quno.qunobackend.interfaces.api.comment

import com.quno.qunobackend.domain.comment.MAX_COMMENT_BODY_LENGTH
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

data class CreateCommentRequest(
    @field:NotBlank
    @field:Size(max = MAX_COMMENT_BODY_LENGTH)
    val body: String,
)
