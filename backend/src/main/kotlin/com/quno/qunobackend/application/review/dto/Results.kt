package com.quno.qunobackend.application.review.dto

import com.quno.qunobackend.domain.review.ReviewRequestStatus
import java.time.Instant

data class ReviewRequestResult(
    val id: Long,
    val questionId: Long,
    val requestedBy: Long,
    val message: String,
    val status: ReviewRequestStatus,
    val questionVersionNumberAtRequest: Int,
    val createdAt: Instant,
    val addressedAt: Instant?,
)
