package com.quno.qunobackend.application.question.usecase

import com.quno.qunobackend.application.common.InMemoryOutboxEventRepository
import com.quno.qunobackend.application.question.dto.CreateQuestionCommand
import com.quno.qunobackend.application.question.dto.MarkQuestionOutdatedCommand
import com.quno.qunobackend.application.tag.usecase.InMemoryQuestionTagRepository
import com.quno.qunobackend.application.tag.usecase.InMemoryTagRepository
import com.quno.qunobackend.domain.common.OutboxEventTypes
import com.quno.qunobackend.domain.question.QuestionNotFoundException
import com.quno.qunobackend.domain.question.QuestionStatus
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class MarkQuestionOutdatedUseCaseTest {
    private val questionRepository = InMemoryQuestionRepository()
    private val questionVersionRepository = InMemoryQuestionVersionRepository()
    private val tagRepository = InMemoryTagRepository()
    private val outboxEventRepository = InMemoryOutboxEventRepository()

    private val createQuestionUseCase = CreateQuestionUseCase(
        questionRepository, questionVersionRepository, tagRepository, InMemoryQuestionTagRepository(tagRepository),
    )
    private val useCase = MarkQuestionOutdatedUseCase(questionRepository, questionVersionRepository, outboxEventRepository)

    private fun questionAskedBy(authorId: Long): Long =
        createQuestionUseCase.execute(
            CreateQuestionCommand(authorId = authorId, title = "t", body = "body", environment = null, logs = null),
        ).id

    @Test
    fun `marks a question outdated`() {
        val questionId = questionAskedBy(1L)

        val result = useCase.execute(MarkQuestionOutdatedCommand(questionId, actorId = 2L, reason = "Spring Boot 4 changed this API"))

        assertEquals(QuestionStatus.OUTDATED, result.status)
        assertEquals(QuestionStatus.OUTDATED, questionRepository.findById(questionId)!!.status)
    }

    @Test
    fun `the author can mark their own question outdated`() {
        val questionId = questionAskedBy(1L)

        val result = useCase.execute(MarkQuestionOutdatedCommand(questionId, actorId = 1L, reason = "no longer applies"))

        assertEquals(QuestionStatus.OUTDATED, result.status)
    }

    @Test
    fun `rejects a question that does not exist`() {
        assertFailsWith<QuestionNotFoundException> {
            useCase.execute(MarkQuestionOutdatedCommand(999L, actorId = 1L, reason = "reason"))
        }
    }

    @Test
    fun `records a QUESTION_OUTDATED outbox event and never notifies the actor`() {
        val questionId = questionAskedBy(1L)

        useCase.execute(MarkQuestionOutdatedCommand(questionId, actorId = 2L, reason = """contains "quotes" and a\backslash"""))

        val event = outboxEventRepository.events.single { it.eventType == OutboxEventTypes.QUESTION_OUTDATED }
        assertEquals(questionId, event.aggregateId)
        assertTrue(event.payload.contains("\"actorId\":2"))
        assertTrue(event.payload.contains("\"questionAuthorId\":1"))
    }
}
