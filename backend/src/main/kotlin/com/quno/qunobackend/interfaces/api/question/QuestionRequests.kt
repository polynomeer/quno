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
)
