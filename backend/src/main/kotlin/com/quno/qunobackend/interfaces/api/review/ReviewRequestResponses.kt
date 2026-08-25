package com.quno.qunobackend.interfaces.api.review

import com.quno.qunobackend.application.review.dto.ReviewRequestResult
import com.quno.qunobackend.domain.review.ReviewRequestStatus
import java.time.Instant

data class ReviewRequestResponse(
    val id: Long,
    val questionId: Long,
    val requestedBy: Long,
    val message: String,
    val status: ReviewRequestStatus,
    val questionVersionNumberAtRequest: Int,
    val createdAt: Instant,
    val addressedAt: Instant?,
)

fun ReviewRequestResult.toResponse() = ReviewRequestResponse(
    id = id,
    questionId = questionId,
    requestedBy = requestedBy,
    message = message,
    status = status,
    questionVersionNumberAtRequest = questionVersionNumberAtRequest,
    createdAt = createdAt,
    addressedAt = addressedAt,
)
