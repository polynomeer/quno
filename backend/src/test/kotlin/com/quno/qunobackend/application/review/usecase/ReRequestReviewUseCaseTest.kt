package com.quno.qunobackend.application.review.usecase

import com.quno.qunobackend.application.common.InMemoryOutboxEventRepository
import com.quno.qunobackend.application.question.dto.CreateQuestionCommand
import com.quno.qunobackend.application.question.dto.ReviseQuestionCommand
import com.quno.qunobackend.application.question.usecase.CreateQuestionUseCase
import com.quno.qunobackend.application.question.usecase.InMemoryQuestionRepository
import com.quno.qunobackend.application.question.usecase.InMemoryQuestionVersionRepository
import com.quno.qunobackend.application.question.usecase.ReviseQuestionUseCase
import com.quno.qunobackend.application.review.dto.CreateReviewRequestCommand
import com.quno.qunobackend.application.review.dto.ReRequestReviewCommand
import com.quno.qunobackend.application.tag.usecase.InMemoryQuestionTagRepository
import com.quno.qunobackend.application.tag.usecase.InMemoryTagRepository
import com.quno.qunobackend.domain.common.OutboxEventTypes
import com.quno.qunobackend.domain.question.QuestionAccessDeniedException
import com.quno.qunobackend.domain.question.QuestionStatus
import com.quno.qunobackend.domain.review.QuestionNotRevisedSinceRequestException
import com.quno.qunobackend.domain.review.ReviewRequestAlreadyAddressedException
import com.quno.qunobackend.domain.review.ReviewRequestNotFoundException
import com.quno.qunobackend.domain.review.ReviewRequestStatus
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class ReRequestReviewUseCaseTest {
    private val questionRepository = InMemoryQuestionRepository()
    private val questionVersionRepository = InMemoryQuestionVersionRepository()
    private val tagRepository = InMemoryTagRepository()
    private val reviewRequestRepository = InMemoryReviewRequestRepository()
    private val outboxEventRepository = InMemoryOutboxEventRepository()

    private val createQuestionUseCase = CreateQuestionUseCase(
        questionRepository, questionVersionRepository, tagRepository, InMemoryQuestionTagRepository(tagRepository),
    )
    private val reviseQuestionUseCase = ReviseQuestionUseCase(questionRepository, questionVersionRepository, outboxEventRepository)
    private val createReviewRequestUseCase = CreateReviewRequestUseCase(
        questionRepository, questionVersionRepository, reviewRequestRepository, outboxEventRepository,
    )
    private val useCase = ReRequestReviewUseCase(questionRepository, questionVersionRepository, reviewRequestRepository, outboxEventRepository)

    private fun questionAskedBy(authorId: Long): Long =
        createQuestionUseCase.execute(
            CreateQuestionCommand(authorId = authorId, title = "t", body = "body", environment = null, logs = null),
        ).id

    @Test
    fun `re-requesting after a revision addresses that specific request`() {
        val questionId = questionAskedBy(authorId = 1L)
        val reviewRequest = createReviewRequestUseCase.execute(CreateReviewRequestCommand(questionId, requestedBy = 2L, message = "add logs"))
        reviseQuestionUseCase.execute(ReviseQuestionCommand(questionId, actorId = 1L, title = "t", body = "body v2", environment = null, logs = "log"))

        val result = useCase.execute(ReRequestReviewCommand(questionId, reviewRequest.id, actorId = 1L))

        assertEquals(ReviewRequestStatus.ADDRESSED, result.status)
        // A revision always exits NEEDS_INFO on its own (Question.revise()) — re-request only
        // marks the reviewer's own thread, it doesn't touch Question.status a second time.
        assertEquals(QuestionStatus.UPDATED, questionRepository.findById(questionId)!!.status)
    }

    @Test
    fun `addressing one review request leaves another reviewer's request open`() {
        val questionId = questionAskedBy(authorId = 1L)
        val first = createReviewRequestUseCase.execute(CreateReviewRequestCommand(questionId, requestedBy = 2L, message = "add logs"))
        val second = createReviewRequestUseCase.execute(CreateReviewRequestCommand(questionId, requestedBy = 3L, message = "add repro"))
        reviseQuestionUseCase.execute(ReviseQuestionCommand(questionId, actorId = 1L, title = "t", body = "body v2", environment = null, logs = "log"))

        useCase.execute(ReRequestReviewCommand(questionId, first.id, actorId = 1L))

        val requests = reviewRequestRepository.findAllByQuestionId(questionId).associateBy { it.id }
        assertEquals(ReviewRequestStatus.ADDRESSED, requests.getValue(first.id).status)
        assertEquals(ReviewRequestStatus.OPEN, requests.getValue(second.id).status)
    }

    @Test
    fun `rejects re-request before any revision happened`() {
        val questionId = questionAskedBy(authorId = 1L)
        val reviewRequest = createReviewRequestUseCase.execute(CreateReviewRequestCommand(questionId, requestedBy = 2L, message = "add logs"))

        assertFailsWith<QuestionNotRevisedSinceRequestException> {
            useCase.execute(ReRequestReviewCommand(questionId, reviewRequest.id, actorId = 1L))
        }
    }

    @Test
    fun `rejects re-request from a non-author`() {
        val questionId = questionAskedBy(authorId = 1L)
        val reviewRequest = createReviewRequestUseCase.execute(CreateReviewRequestCommand(questionId, requestedBy = 2L, message = "add logs"))
        reviseQuestionUseCase.execute(ReviseQuestionCommand(questionId, actorId = 1L, title = "t", body = "body v2", environment = null, logs = "log"))

        assertFailsWith<QuestionAccessDeniedException> {
            useCase.execute(ReRequestReviewCommand(questionId, reviewRequest.id, actorId = 2L))
        }
    }

    @Test
    fun `rejects re-requesting an already addressed review request`() {
        val questionId = questionAskedBy(authorId = 1L)
        val reviewRequest = createReviewRequestUseCase.execute(CreateReviewRequestCommand(questionId, requestedBy = 2L, message = "add logs"))
        reviseQuestionUseCase.execute(ReviseQuestionCommand(questionId, actorId = 1L, title = "t", body = "body v2", environment = null, logs = "log"))
        useCase.execute(ReRequestReviewCommand(questionId, reviewRequest.id, actorId = 1L))

        assertFailsWith<ReviewRequestAlreadyAddressedException> {
            useCase.execute(ReRequestReviewCommand(questionId, reviewRequest.id, actorId = 1L))
        }
    }

    @Test
    fun `rejects a review request id that does not exist`() {
        val questionId = questionAskedBy(authorId = 1L)

        assertFailsWith<ReviewRequestNotFoundException> {
            useCase.execute(ReRequestReviewCommand(questionId, reviewRequestId = 999L, actorId = 1L))
        }
    }

    @Test
    fun `records a REVIEW_RE_REQUESTED outbox event`() {
        val questionId = questionAskedBy(authorId = 1L)
        val reviewRequest = createReviewRequestUseCase.execute(CreateReviewRequestCommand(questionId, requestedBy = 2L, message = "add logs"))
        reviseQuestionUseCase.execute(ReviseQuestionCommand(questionId, actorId = 1L, title = "t", body = "body v2", environment = null, logs = "log"))

        useCase.execute(ReRequestReviewCommand(questionId, reviewRequest.id, actorId = 1L))

        assertTrue(
            outboxEventRepository.events.any {
                it.eventType == OutboxEventTypes.REVIEW_RE_REQUESTED && it.aggregateId == questionId
            },
        )
    }
}
