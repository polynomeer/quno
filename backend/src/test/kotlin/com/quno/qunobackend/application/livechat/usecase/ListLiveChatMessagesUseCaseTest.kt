package com.quno.qunobackend.application.livechat.usecase

import com.quno.qunobackend.domain.livechat.LiveChatRoom
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class ListLiveChatMessagesUseCaseTest {
    private val liveChatRoomRepository = InMemoryLiveChatRoomRepository()
    private val liveChatMessageRepository = InMemoryLiveChatMessageRepository()
    private val postUseCase = PostLiveChatMessageUseCase(liveChatRoomRepository, liveChatMessageRepository)
    private val useCase = ListLiveChatMessagesUseCase(liveChatMessageRepository)

    @Test
    fun `returns messages oldest first`() {
        val room = liveChatRoomRepository.save(
            LiveChatRoom.open(questionId = 1L, createdBy = 1L),
        )
        val roomId = requireNotNull(room.id)
        postUseCase.execute(roomId, senderId = 1L, body = "first")
        postUseCase.execute(roomId, senderId = 2L, body = "second")
        postUseCase.execute(roomId, senderId = 1L, body = "third")

        val messages = useCase.execute(roomId, limit = 10)

        assertEquals(listOf("first", "second", "third"), messages.map { it.body })
    }

    @Test
    fun `limit caps how many recent messages come back`() {
        val room = liveChatRoomRepository.save(
            LiveChatRoom.open(questionId = 1L, createdBy = 1L),
        )
        val roomId = requireNotNull(room.id)
        repeat(5) { postUseCase.execute(roomId, senderId = 1L, body = "msg $it") }

        val messages = useCase.execute(roomId, limit = 2)

        assertEquals(listOf("msg 3", "msg 4"), messages.map { it.body })
    }
}
