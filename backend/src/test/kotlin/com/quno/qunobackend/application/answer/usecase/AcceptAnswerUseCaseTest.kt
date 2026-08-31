package com.quno.qunobackend.application.answer.usecase

import com.quno.qunobackend.application.answer.dto.AcceptAnswerCommand
import com.quno.qunobackend.application.answer.dto.WriteAnswerCommand
import com.quno.qunobackend.application.common.AnswerResultAssembler
import com.quno.qunobackend.application.common.InMemoryOutboxEventRepository
import com.quno.qunobackend.application.question.dto.CreateQuestionCommand
import com.quno.qunobackend.application.question.usecase.CreateQuestionUseCase
import com.quno.qunobackend.application.question.usecase.InMemoryQuestionRepository
import com.quno.qunobackend.application.question.usecase.InMemoryQuestionVersionRepository
import com.quno.qunobackend.application.tag.usecase.InMemoryQuestionTagRepository
import com.quno.qunobackend.application.tag.usecase.InMemoryTagRepository
import com.quno.qunobackend.application.vote.usecase.InMemoryVoteRepository
import com.quno.qunobackend.domain.common.OutboxEventTypes
import com.quno.qunobackend.domain.question.QuestionAccessDeniedException
import com.quno.qunobackend.domain.question.QuestionStatus
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import org.junit.jupiter.api.Test

class AcceptAnswerUseCaseTest {
    private val questionRepository = InMemoryQuestionRepository()
    private val questionVersionRepository = InMemoryQuestionVersionRepository()
    private val answerRepository = InMemoryAnswerRepository()
    private val tagRepository = InMemoryTagRepository()
    private val outboxEventRepository = InMemoryOutboxEventRepository()
    private val createQuestionUseCase = CreateQuestionUseCase(
        questionRepository, questionVersionRepository, tagRepository, InMemoryQuestionTagRepository(tagRepository),
    )
    private val writeAnswerUseCase = WriteAnswerUseCase(
        questionRepository,
        questionVersionRepository,
        answerRepository,
        InMemoryAnswerVersionRepository(),
        outboxEventRepository,
        AnswerResultAssembler(questionRepository, questionVersionRepository, InMemoryVoteRepository()),
    )
    private val acceptAnswerUseCase = AcceptAnswerUseCase(questionRepository, answerRepository, outboxEventRepository)

    private fun questionAskedBy(authorId: Long): Long =
        createQuestionUseCase.execute(
            CreateQuestionCommand(authorId = authorId, title = "t", body = "body", environment = null, logs = null),
        ).id

    @Test
    fun `accepting an answer resolves the question`() {
        val questionId = questionAskedBy(authorId = 1L)
        val answer = writeAnswerUseCase.execute(WriteAnswerCommand(questionId, authorId = 2L, body = "Try this."))

        val result = acceptAnswerUseCase.execute(AcceptAnswerCommand(answerId = answer.id, actorId = 1L))

        assertEquals(QuestionStatus.RESOLVED, result.questionStatus)
        assertTrue(answerRepository.findById(answer.id)!!.isAccepted)
    }

    @Test
    fun `accepting a new answer unaccepts the previous one`() {
        val questionId = questionAskedBy(authorId = 1L)
        val first = writeAnswerUseCase.execute(WriteAnswerCommand(questionId, authorId = 2L, body = "First."))
        val second = writeAnswerUseCase.execute(WriteAnswerCommand(questionId, authorId = 3L, body = "Second."))

        acceptAnswerUseCase.execute(AcceptAnswerCommand(answerId = first.id, actorId = 1L))
        acceptAnswerUseCase.execute(AcceptAnswerCommand(answerId = second.id, actorId = 1L))

        assertFalse(answerRepository.findById(first.id)!!.isAccepted)
        assertTrue(answerRepository.findById(second.id)!!.isAccepted)
    }

    @Test
    fun `only the question author can accept an answer`() {
        val questionId = questionAskedBy(authorId = 1L)
        val answer = writeAnswerUseCase.execute(WriteAnswerCommand(questionId, authorId = 2L, body = "Try this."))

        assertFailsWith<QuestionAccessDeniedException> {
            acceptAnswerUseCase.execute(AcceptAnswerCommand(answerId = answer.id, actorId = 2L))
        }
    }

    @Test
    fun `records an ANSWER_ACCEPTED outbox event`() {
        val questionId = questionAskedBy(authorId = 1L)
        val answer = writeAnswerUseCase.execute(WriteAnswerCommand(questionId, authorId = 2L, body = "Try this."))

        acceptAnswerUseCase.execute(AcceptAnswerCommand(answerId = answer.id, actorId = 1L))

        assertTrue(
            outboxEventRepository.events.any {
                it.eventType == OutboxEventTypes.ANSWER_ACCEPTED && it.aggregateId == questionId
            },
        )
    }
}
