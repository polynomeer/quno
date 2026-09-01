package com.quno.qunobackend.application.livechat.usecase

import com.quno.qunobackend.domain.livechat.LiveChatRoom
import com.quno.qunobackend.domain.livechat.LiveChatRoomNotFoundException
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class PostLiveChatMessageUseCaseTest {
    private val liveChatRoomRepository = InMemoryLiveChatRoomRepository()
    private val liveChatMessageRepository = InMemoryLiveChatMessageRepository()
    private val useCase = PostLiveChatMessageUseCase(liveChatRoomRepository, liveChatMessageRepository)

    @Test
    fun `posts a message to an existing room`() {
        val room = liveChatRoomRepository.save(LiveChatRoom.open(questionId = 1L, createdBy = 1L))

        val result = useCase.execute(requireNotNull(room.id), senderId = 2L, body = "hello")

        assertEquals("hello", result.body)
        assertEquals(2L, result.senderId)
        val stored = liveChatMessageRepository.findRecentByRoomId(requireNotNull(room.id), 10).single()
        assertEquals(result.id, stored.id)
        assertEquals("hello", stored.body)
    }

    @Test
    fun `posting to a room that does not exist fails`() {
        assertFailsWith<LiveChatRoomNotFoundException> { useCase.execute(999L, senderId = 1L, body = "hello") }
    }

    @Test
    fun `posting a blank message fails`() {
        val room = liveChatRoomRepository.save(LiveChatRoom.open(questionId = 1L, createdBy = 1L))

        assertFailsWith<IllegalArgumentException> { useCase.execute(requireNotNull(room.id), senderId = 2L, body = "   ") }
    }
}
