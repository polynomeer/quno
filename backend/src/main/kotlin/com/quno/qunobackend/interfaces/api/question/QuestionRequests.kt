package com.quno.qunobackend.interfaces.api.question

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

/** Shared shape for both creating Qv1 and appending a revision (Qv2+). */
data class QuestionContentRequest(
    @field:NotBlank @field:Size(max = 300)
    val title: String,
    @field:NotBlank
    val body: String,
    val environment: String? = null,
    val logs: String? = null,
)
