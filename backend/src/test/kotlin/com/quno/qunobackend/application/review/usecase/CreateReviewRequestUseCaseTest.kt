package com.quno.qunobackend.application.review.usecase

import com.quno.qunobackend.application.answer.dto.AcceptAnswerCommand
import com.quno.qunobackend.application.answer.usecase.AcceptAnswerUseCase
import com.quno.qunobackend.application.answer.usecase.InMemoryAnswerRepository
import com.quno.qunobackend.application.answer.usecase.WriteAnswerUseCase
import com.quno.qunobackend.application.answer.dto.WriteAnswerCommand
import com.quno.qunobackend.application.common.AnswerResultAssembler
import com.quno.qunobackend.application.common.InMemoryOutboxEventRepository
import com.quno.qunobackend.application.question.dto.CreateQuestionCommand
import com.quno.qunobackend.application.question.usecase.CreateQuestionUseCase
import com.quno.qunobackend.application.question.usecase.InMemoryQuestionRepository
import com.quno.qunobackend.application.question.usecase.InMemoryQuestionVersionRepository
import com.quno.qunobackend.application.review.dto.CreateReviewRequestCommand
import com.quno.qunobackend.application.tag.usecase.InMemoryQuestionTagRepository
import com.quno.qunobackend.application.tag.usecase.InMemoryTagRepository
import com.quno.qunobackend.domain.common.OutboxEventTypes
import com.quno.qunobackend.domain.question.QuestionAlreadyResolvedException
import com.quno.qunobackend.domain.question.QuestionNotFoundException
import com.quno.qunobackend.domain.question.QuestionStatus
import com.quno.qunobackend.domain.review.ReviewRequestStatus
import com.quno.qunobackend.domain.review.SelfReviewRequestException
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class CreateReviewRequestUseCaseTest {
    private val questionRepository = InMemoryQuestionRepository()
    private val questionVersionRepository = InMemoryQuestionVersionRepository()
    private val tagRepository = InMemoryTagRepository()
    private val answerRepository = InMemoryAnswerRepository()
    private val reviewRequestRepository = InMemoryReviewRequestRepository()
    private val outboxEventRepository = InMemoryOutboxEventRepository()

    private val createQuestionUseCase = CreateQuestionUseCase(
        questionRepository, questionVersionRepository, tagRepository, InMemoryQuestionTagRepository(tagRepository),
    )
    private val writeAnswerUseCase = WriteAnswerUseCase(
        questionRepository, questionVersionRepository, answerRepository, outboxEventRepository,
        AnswerResultAssembler(questionRepository, questionVersionRepository),
    )
    private val acceptAnswerUseCase = AcceptAnswerUseCase(questionRepository, answerRepository, outboxEventRepository)
    private val useCase = CreateReviewRequestUseCase(
        questionRepository, questionVersionRepository, reviewRequestRepository, outboxEventRepository,
    )

    private fun questionAskedBy(authorId: Long): Long =
        createQuestionUseCase.execute(
            CreateQuestionCommand(authorId = authorId, title = "t", body = "body", environment = null, logs = null),
        ).id

    @Test
    fun `opens a review request and moves the question to NEEDS_INFO`() {
        val questionId = questionAskedBy(authorId = 1L)

        val result = useCase.execute(CreateReviewRequestCommand(questionId, requestedBy = 2L, message = "please add logs"))

        assertEquals(ReviewRequestStatus.OPEN, result.status)
        assertEquals(1, result.questionVersionNumberAtRequest)
        assertEquals(QuestionStatus.NEEDS_INFO, questionRepository.findById(questionId)!!.status)
    }

    @Test
    fun `a second open request from a different reviewer is independent and question stays NEEDS_INFO`() {
        val questionId = questionAskedBy(authorId = 1L)
        useCase.execute(CreateReviewRequestCommand(questionId, requestedBy = 2L, message = "please add logs"))

        val second = useCase.execute(CreateReviewRequestCommand(questionId, requestedBy = 3L, message = "please add repro steps"))

        assertEquals(2, reviewRequestRepository.findAllByQuestionId(questionId).size)
        assertEquals(ReviewRequestStatus.OPEN, second.status)
        assertEquals(QuestionStatus.NEEDS_INFO, questionRepository.findById(questionId)!!.status)
    }

    @Test
    fun `rejects a review request from the question's own author`() {
        val questionId = questionAskedBy(authorId = 1L)

        assertFailsWith<SelfReviewRequestException> {
            useCase.execute(CreateReviewRequestCommand(questionId, requestedBy = 1L, message = "please add logs"))
        }
    }

    @Test
    fun `rejects a review request on a resolved question`() {
        val questionId = questionAskedBy(authorId = 1L)
        val answer = writeAnswerUseCase.execute(WriteAnswerCommand(questionId, authorId = 2L, body = "answer"))
        acceptAnswerUseCase.execute(AcceptAnswerCommand(answerId = answer.id, actorId = 1L))

        assertFailsWith<QuestionAlreadyResolvedException> {
            useCase.execute(CreateReviewRequestCommand(questionId, requestedBy = 2L, message = "please add logs"))
        }
    }

    @Test
    fun `rejects a review request for a question that does not exist`() {
        assertFailsWith<QuestionNotFoundException> {
            useCase.execute(CreateReviewRequestCommand(999L, requestedBy = 2L, message = "please add logs"))
        }
    }

    @Test
    fun `records a REVIEW_REQUESTED outbox event`() {
        val questionId = questionAskedBy(authorId = 1L)

        useCase.execute(CreateReviewRequestCommand(questionId, requestedBy = 2L, message = "please add logs"))

        assertTrue(
            outboxEventRepository.events.any {
                it.eventType == OutboxEventTypes.REVIEW_REQUESTED && it.aggregateId == questionId
            },
        )
    }
}
