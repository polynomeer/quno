package com.quno.qunobackend.interfaces.api.review

import com.quno.qunobackend.application.review.dto.ReviewRequestResult
import com.quno.qunobackend.domain.review.ReviewRequestStatus
import java.time.Instant

data class ReviewRequestResponse(
    val id: Long,
    val questionId: Long,
    val requestedBy: Long,
    /** Filled by the list endpoint (ADR-0064); null where a response isn't enriched (e.g. create). */
    val requestedByNickname: String?,
    val message: String,
    val status: ReviewRequestStatus,
    val questionVersionNumberAtRequest: Int,
    val createdAt: Instant,
    val addressedAt: Instant?,
)

fun ReviewRequestResult.toResponse(requestedByNickname: String? = null) = ReviewRequestResponse(
    id = id,
    questionId = questionId,
    requestedBy = requestedBy,
    requestedByNickname = requestedByNickname,
    message = message,
    status = status,
    questionVersionNumberAtRequest = questionVersionNumberAtRequest,
    createdAt = createdAt,
    addressedAt = addressedAt,
)
