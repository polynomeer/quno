package com.quno.qunobackend.application.review.usecase

import com.quno.qunobackend.application.review.dto.CreateReviewRequestCommand
import com.quno.qunobackend.application.review.dto.ReviewRequestResult
import com.quno.qunobackend.domain.common.OutboxEvent
import com.quno.qunobackend.domain.common.OutboxEventRepository
import com.quno.qunobackend.domain.common.OutboxEventTypes
import com.quno.qunobackend.domain.question.QuestionNotFoundException
import com.quno.qunobackend.domain.question.QuestionRepository
import com.quno.qunobackend.domain.question.QuestionVersionRepository
import com.quno.qunobackend.domain.review.ReviewRequest
import com.quno.qunobackend.domain.review.ReviewRequestRepository
import com.quno.qunobackend.domain.review.SelfReviewRequestException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class CreateReviewRequestUseCase(
    private val questionRepository: QuestionRepository,
    private val questionVersionRepository: QuestionVersionRepository,
    private val reviewRequestRepository: ReviewRequestRepository,
    private val outboxEventRepository: OutboxEventRepository,
) {
    @Transactional
    fun execute(command: CreateReviewRequestCommand): ReviewRequestResult {
        val question = questionRepository.findById(command.questionId)
            ?: throw QuestionNotFoundException(command.questionId)
        if (question.authorId == command.requestedBy) throw SelfReviewRequestException(command.questionId)

        // requestMoreInfo() throws QuestionAlreadyResolvedException for a RESOLVED question.
        questionRepository.save(question.requestMoreInfo())

        val versionNumber = requireNotNull(questionVersionRepository.findById(requireNotNull(question.latestVersionId))).versionNumber
        val saved = reviewRequestRepository.save(
            ReviewRequest.request(
                questionId = command.questionId,
                requestedBy = command.requestedBy,
                message = command.message,
                questionVersionNumberAtRequest = versionNumber,
            ),
        )

        // questionAuthorId: the question's author is always notified, even if they never
        // explicitly watched their own question — see DispatchOutboxEventsUseCase's kdoc.
        outboxEventRepository.save(
            OutboxEvent.create(
                eventType = OutboxEventTypes.REVIEW_REQUESTED,
                aggregateType = "QUESTION",
                aggregateId = command.questionId,
                payload = """{"reviewRequestId":${saved.id},"actorId":${command.requestedBy},"questionAuthorId":${question.authorId}}""",
            ),
        )

        return saved.toResult()
    }
}

internal fun ReviewRequest.toResult() = ReviewRequestResult(
    id = requireNotNull(id),
    questionId = questionId,
    requestedBy = requestedBy,
    message = message,
    status = status,
    questionVersionNumberAtRequest = questionVersionNumberAtRequest,
    createdAt = createdAt,
    addressedAt = addressedAt,
)
