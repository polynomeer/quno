package com.quno.qunobackend.interfaces.api.review

import jakarta.validation.constraints.NotBlank

data class CreateReviewRequestRequest(
    @field:NotBlank
    val message: String,
)
