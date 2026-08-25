package com.quno.qunobackend.application.review.usecase

import com.quno.qunobackend.domain.review.ReviewRequest
import com.quno.qunobackend.domain.review.ReviewRequestRepository

class InMemoryReviewRequestRepository : ReviewRequestRepository {
    private val byId = mutableMapOf<Long, ReviewRequest>()
    private var nextId = 1L

    override fun save(reviewRequest: ReviewRequest): ReviewRequest {
        val saved = if (reviewRequest.id == null) {
            ReviewRequest.reconstitute(
                id = nextId++,
                questionId = reviewRequest.questionId,
                requestedBy = reviewRequest.requestedBy,
                message = reviewRequest.message,
                status = reviewRequest.status,
                questionVersionNumberAtRequest = reviewRequest.questionVersionNumberAtRequest,
                createdAt = reviewRequest.createdAt,
                addressedAt = reviewRequest.addressedAt,
            )
        } else {
            reviewRequest
        }
        byId[requireNotNull(saved.id)] = saved
        return saved
    }

    override fun findById(id: Long): ReviewRequest? = byId[id]

    override fun findAllByQuestionId(questionId: Long): List<ReviewRequest> =
        byId.values.filter { it.questionId == questionId }.sortedByDescending { it.createdAt }
}
