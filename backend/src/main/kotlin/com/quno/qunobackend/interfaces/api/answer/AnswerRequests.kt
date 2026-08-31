package com.quno.qunobackend.interfaces.api.answer

import jakarta.validation.constraints.NotBlank

data class WriteAnswerRequest(
    @field:NotBlank
    val body: String,
)

/** Revising an answer only ever touches its body — there's no title/environment/logs to carry. */
data class AnswerContentRequest(
    @field:NotBlank
    val body: String,
)
