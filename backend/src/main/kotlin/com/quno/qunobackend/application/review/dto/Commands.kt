package com.quno.qunobackend.application.review.dto

data class CreateReviewRequestCommand(
    val questionId: Long,
    val requestedBy: Long,
    val message: String,
)
