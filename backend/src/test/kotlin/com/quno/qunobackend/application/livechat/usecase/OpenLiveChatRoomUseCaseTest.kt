package com.quno.qunobackend.application.livechat.usecase

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
import kotlin.test.assertTrue

class OpenLiveChatRoomUseCaseTest {
    private val questionRepository = InMemoryQuestionRepository()
    private val questionVersionRepository = InMemoryQuestionVersionRepository()
    private val tagRepository = InMemoryTagRepository()
    private val liveChatRoomRepository = InMemoryLiveChatRoomRepository()
    private val outboxEventRepository = InMemoryOutboxEventRepository()

    private val createQuestionUseCase = CreateQuestionUseCase(
        questionRepository, questionVersionRepository, tagRepository, InMemoryQuestionTagRepository(tagRepository),
    )
    private val useCase = OpenLiveChatRoomUseCase(questionRepository, liveChatRoomRepository, outboxEventRepository)

    private fun questionAskedBy(authorId: Long): Long =
        createQuestionUseCase.execute(CreateQuestionCommand(authorId, "t", "body", null, null)).id

    @Test
    fun `opening a room for the first time creates it and notifies the question's author`() {
        val questionId = questionAskedBy(1L)

        val result = useCase.execute(questionId, userId = 2L)

        assertEquals(questionId, result.questionId)
        assertEquals(2L, result.createdBy)
        val event = outboxEventRepository.events.single { it.eventType == OutboxEventTypes.LIVE_CHAT_STARTED }
        assertTrue(event.payload.contains("\"questionAuthorId\":1"))
        assertTrue(event.payload.contains("\"actorId\":2"))
    }

    @Test
    fun `opening an already-open room returns the same room and does not notify again`() {
        val questionId = questionAskedBy(1L)
        val first = useCase.execute(questionId, userId = 2L)

        val second = useCase.execute(questionId, userId = 3L)

        assertEquals(first.id, second.id)
        assertEquals(2L, second.createdBy)
        assertEquals(1, outboxEventRepository.events.count { it.eventType == OutboxEventTypes.LIVE_CHAT_STARTED })
    }

    @Test
    fun `opening a room for a question that does not exist fails`() {
        assertFailsWith<QuestionNotFoundException> { useCase.execute(999L, userId = 1L) }
    }
}
