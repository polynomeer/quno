package com.quno.qunobackend.infrastructure.persistence.jpa.adapter

import com.quno.qunobackend.domain.review.ReviewRequest
import com.quno.qunobackend.domain.review.ReviewRequestRepository
import com.quno.qunobackend.infrastructure.persistence.jpa.entity.ReviewRequestJpaEntity
import com.quno.qunobackend.infrastructure.persistence.jpa.repository.ReviewRequestJpaRepository
import org.springframework.stereotype.Component

@Component
class ReviewRequestRepositoryAdapter(
    private val jpaRepository: ReviewRequestJpaRepository,
) : ReviewRequestRepository {

    override fun save(reviewRequest: ReviewRequest): ReviewRequest {
        val entity = ReviewRequestJpaEntity(
            id = reviewRequest.id,
            questionId = reviewRequest.questionId,
            requestedBy = reviewRequest.requestedBy,
            message = reviewRequest.message,
            status = reviewRequest.status,
            questionVersionNumberAtRequest = reviewRequest.questionVersionNumberAtRequest,
            createdAt = reviewRequest.createdAt,
            addressedAt = reviewRequest.addressedAt,
        )
        return jpaRepository.save(entity).toDomain()
    }

    override fun findAllByQuestionId(questionId: Long): List<ReviewRequest> =
        jpaRepository.findAllByQuestionIdOrderByCreatedAtDesc(questionId).map { it.toDomain() }

    private fun ReviewRequestJpaEntity.toDomain(): ReviewRequest = ReviewRequest.reconstitute(
        id = requireNotNull(id),
        questionId = questionId,
        requestedBy = requestedBy,
        message = message,
        status = status,
        questionVersionNumberAtRequest = questionVersionNumberAtRequest,
        createdAt = createdAt,
        addressedAt = addressedAt,
    )
}
