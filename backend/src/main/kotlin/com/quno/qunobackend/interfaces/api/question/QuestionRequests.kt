package com.quno.qunobackend.interfaces.api.question

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

data class CreateQuestionRequest(
    @field:NotBlank @field:Size(max = 300)
    val title: String,
    @field:NotBlank
    val body: String,
    val environment: String? = null,
    val logs: String? = null,
    val tags: List<String> = emptyList(),
)

/** Revising a question doesn't touch its tags in MVP — only content fields. */
data class QuestionContentRequest(
    @field:NotBlank @field:Size(max = 300)
    val title: String,
    @field:NotBlank
    val body: String,
    val environment: String? = null,
    val logs: String? = null,
)
