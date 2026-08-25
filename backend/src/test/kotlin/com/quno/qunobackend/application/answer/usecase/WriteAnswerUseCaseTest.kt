package com.quno.qunobackend.application.answer.usecase

import com.quno.qunobackend.application.answer.dto.WriteAnswerCommand
import com.quno.qunobackend.application.common.AnswerResultAssembler
import com.quno.qunobackend.application.common.InMemoryOutboxEventRepository
import com.quno.qunobackend.application.question.dto.CreateQuestionCommand
import com.quno.qunobackend.application.question.usecase.CreateQuestionUseCase
import com.quno.qunobackend.application.question.usecase.InMemoryQuestionRepository
import com.quno.qunobackend.application.question.usecase.InMemoryQuestionVersionRepository
import com.quno.qunobackend.application.tag.usecase.InMemoryQuestionTagRepository
import com.quno.qunobackend.application.tag.usecase.InMemoryTagRepository
import com.quno.qunobackend.domain.common.OutboxEventTypes
import com.quno.qunobackend.domain.question.QuestionNotFoundException
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class WriteAnswerUseCaseTest {
    private val questionRepository = InMemoryQuestionRepository()
    private val questionVersionRepository = InMemoryQuestionVersionRepository()
    private val answerRepository = InMemoryAnswerRepository()
    private val tagRepository = InMemoryTagRepository()
    private val outboxEventRepository = InMemoryOutboxEventRepository()
    private val createQuestionUseCase = CreateQuestionUseCase(
        questionRepository, questionVersionRepository, tagRepository, InMemoryQuestionTagRepository(tagRepository),
    )
    private val useCase = WriteAnswerUseCase(
        questionRepository,
        questionVersionRepository,
        answerRepository,
        outboxEventRepository,
        AnswerResultAssembler(questionRepository, questionVersionRepository),
    )

    @Test
    fun `writes an unaccepted answer for an existing question`() {
        val questionId = createQuestionUseCase.execute(
            CreateQuestionCommand(authorId = 1L, title = "t", body = "body", environment = null, logs = null),
        ).id

        val result = useCase.execute(WriteAnswerCommand(questionId = questionId, authorId = 2L, body = "Try this."))

        assertEquals("Try this.", result.body)
        assertFalse(result.isAccepted)
        assertEquals(1, result.targetVersionNumber)
        assertFalse(result.isStale)
    }

    @Test
    fun `rejects an answer for a question that does not exist`() {
        assertFailsWith<QuestionNotFoundException> {
            useCase.execute(WriteAnswerCommand(questionId = 999L, authorId = 2L, body = "Try this."))
        }
    }

    @Test
    fun `records a NEW_ANSWER outbox event`() {
        val questionId = createQuestionUseCase.execute(
            CreateQuestionCommand(authorId = 1L, title = "t", body = "body", environment = null, logs = null),
        ).id

        useCase.execute(WriteAnswerCommand(questionId = questionId, authorId = 2L, body = "Try this."))

        assertTrue(
            outboxEventRepository.events.any {
                it.eventType == OutboxEventTypes.NEW_ANSWER && it.aggregateId == questionId
            },
        )
    }
}
