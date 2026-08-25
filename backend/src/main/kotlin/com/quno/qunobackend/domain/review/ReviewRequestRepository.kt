package com.quno.qunobackend.domain.review

/** Port implemented by infrastructure/persistence/jpa/adapter/ReviewRequestRepositoryAdapter. */
interface ReviewRequestRepository {
    fun save(reviewRequest: ReviewRequest): ReviewRequest

    /** Most recent first. */
    fun findAllByQuestionId(questionId: Long): List<ReviewRequest>
}
