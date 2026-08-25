package com.quno.qunobackend.application.review.usecase

import com.quno.qunobackend.application.review.dto.ReviewRequestResult
import com.quno.qunobackend.domain.question.QuestionNotFoundException
import com.quno.qunobackend.domain.question.QuestionRepository
import com.quno.qunobackend.domain.review.ReviewRequestRepository
import org.springframework.stereotype.Service

@Service
class ListReviewRequestsUseCase(
    private val questionRepository: QuestionRepository,
    private val reviewRequestRepository: ReviewRequestRepository,
) {
    fun execute(questionId: Long): List<ReviewRequestResult> {
        questionRepository.findById(questionId) ?: throw QuestionNotFoundException(questionId)
        return reviewRequestRepository.findAllByQuestionId(questionId).map { it.toResult() }
    }
}
