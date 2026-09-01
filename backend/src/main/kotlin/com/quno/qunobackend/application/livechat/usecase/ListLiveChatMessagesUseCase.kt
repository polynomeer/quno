package com.quno.qunobackend.application.livechat.usecase

import com.quno.qunobackend.application.livechat.dto.LiveChatMessageResult
import com.quno.qunobackend.domain.livechat.LiveChatMessageRepository
import org.springframework.stereotype.Service

/** Backs the initial history load before a client's WebSocket subscription starts receiving
 * live messages — REST, not STOMP, so a page refresh doesn't lose scrollback. */
@Service
class ListLiveChatMessagesUseCase(
    private val liveChatMessageRepository: LiveChatMessageRepository,
) {
    fun execute(roomId: Long, limit: Int = 50): List<LiveChatMessageResult> =
        liveChatMessageRepository.findRecentByRoomId(roomId, limit).map { it.toResult() }
}
