package com.quno.qunobackend.interfaces.api.answer

import jakarta.validation.constraints.NotBlank

data class WriteAnswerRequest(
    @field:NotBlank
    val body: String,
)
