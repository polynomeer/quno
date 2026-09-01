package com.quno.qunobackend.application.livechat.usecase

import com.quno.qunobackend.application.livechat.dto.LiveChatMessageResult
import com.quno.qunobackend.domain.livechat.LiveChatMessage
import com.quno.qunobackend.domain.livechat.LiveChatMessageRepository
import com.quno.qunobackend.domain.livechat.LiveChatRoomNotFoundException
import com.quno.qunobackend.domain.livechat.LiveChatRoomRepository
import org.springframework.stereotype.Service

/** Called by the STOMP message handler (infrastructure/websocket), not a REST controller —
 * see LiveChatWebSocketController. No outbox event per message (would be spam). */
@Service
class PostLiveChatMessageUseCase(
    private val liveChatRoomRepository: LiveChatRoomRepository,
    private val liveChatMessageRepository: LiveChatMessageRepository,
) {
    fun execute(roomId: Long, senderId: Long, body: String): LiveChatMessageResult {
        liveChatRoomRepository.findById(roomId) ?: throw LiveChatRoomNotFoundException(roomId)
        val saved = liveChatMessageRepository.save(LiveChatMessage.post(roomId, senderId, body))
        return saved.toResult()
    }
}

internal fun LiveChatMessage.toResult() =
    LiveChatMessageResult(id = requireNotNull(id), roomId = roomId, senderId = senderId, body = body, createdAt = createdAt)
