package com.quno.qunobackend.application.review.usecase

import com.quno.qunobackend.application.review.dto.ReRequestReviewCommand
import com.quno.qunobackend.application.review.dto.ReviewRequestResult
import com.quno.qunobackend.domain.common.OutboxEvent
import com.quno.qunobackend.domain.common.OutboxEventRepository
import com.quno.qunobackend.domain.common.OutboxEventTypes
import com.quno.qunobackend.domain.question.QuestionAccessDeniedException
import com.quno.qunobackend.domain.question.QuestionNotFoundException
import com.quno.qunobackend.domain.question.QuestionRepository
import com.quno.qunobackend.domain.question.QuestionVersionRepository
import com.quno.qunobackend.domain.review.QuestionNotRevisedSinceRequestException
import com.quno.qunobackend.domain.review.ReviewRequestAlreadyAddressedException
import com.quno.qunobackend.domain.review.ReviewRequestNotFoundException
import com.quno.qunobackend.domain.review.ReviewRequestRepository
import com.quno.qunobackend.domain.review.ReviewRequestStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * The question author confirms one reviewer's ReviewRequest has been addressed, typically
 * after revising (matching GitHub's "re-request review" — see PLAN.md 5.3). This only marks
 * that one thread ADDRESSED; it never touches Question.status. Question.status already left
 * NEEDS_INFO the moment the author revised (see Question.revise() — a revision always exits
 * NEEDS_INFO regardless of how many reviewers' threads are still open), so by the time a
 * re-request is even allowed here (it requires a revision past the request's version), the
 * question is already out of NEEDS_INFO. ReviewRequest.status is independent per-reviewer
 * bookkeeping, not a second gate on Question.status — see ADR-0015.
 */
@Service
class ReRequestReviewUseCase(
    private val questionRepository: QuestionRepository,
    private val questionVersionRepository: QuestionVersionRepository,
    private val reviewRequestRepository: ReviewRequestRepository,
    private val outboxEventRepository: OutboxEventRepository,
) {
    @Transactional
    fun execute(command: ReRequestReviewCommand): ReviewRequestResult {
        val question = questionRepository.findById(command.questionId)
            ?: throw QuestionNotFoundException(command.questionId)
        if (question.authorId != command.actorId) throw QuestionAccessDeniedException(command.questionId)

        val reviewRequest = reviewRequestRepository.findById(command.reviewRequestId)
            ?.takeIf { it.questionId == command.questionId }
            ?: throw ReviewRequestNotFoundException(command.reviewRequestId)
        if (reviewRequest.status == ReviewRequestStatus.ADDRESSED) {
            throw ReviewRequestAlreadyAddressedException(command.reviewRequestId)
        }

        val latestVersionNumber = requireNotNull(
            questionVersionRepository.findById(requireNotNull(question.latestVersionId)),
        ).versionNumber
        if (latestVersionNumber <= reviewRequest.questionVersionNumberAtRequest) {
            throw QuestionNotRevisedSinceRequestException(command.reviewRequestId)
        }

        val addressed = reviewRequestRepository.save(reviewRequest.addressed())

        // reviewerId: the original requester is always notified, even if they never
        // explicitly watched the question — see DispatchOutboxEventsUseCase's kdoc.
        outboxEventRepository.save(
            OutboxEvent.create(
                eventType = OutboxEventTypes.REVIEW_RE_REQUESTED,
                aggregateType = "QUESTION",
                aggregateId = command.questionId,
                payload = """{"reviewRequestId":${addressed.id},"actorId":${command.actorId},"reviewerId":${addressed.requestedBy}}""",
            ),
        )

        return addressed.toResult()
    }
}
