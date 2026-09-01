package com.quno.qunobackend.infrastructure.websocket

import com.quno.qunobackend.application.livechat.usecase.PostLiveChatMessageUseCase
import org.springframework.messaging.handler.annotation.DestinationVariable
import org.springframework.messaging.handler.annotation.MessageMapping
import org.springframework.messaging.simp.SimpMessagingTemplate
import org.springframework.stereotype.Controller
import java.security.Principal
import java.time.Instant

data class LiveChatSendMessageRequest(val body: String)

data class LiveChatMessageBroadcast(val id: String, val roomId: Long, val senderId: Long, val body: String, val createdAt: Instant)

/**
 * `@SendTo` can't interpolate `{roomId}` into its destination (it's a static string), so the
 * broadcast is sent explicitly via [SimpMessagingTemplate] to `/topic/live-chat/{roomId}` —
 * clients subscribe there to receive every message posted in that room, including their own.
 */
@Controller
class LiveChatWebSocketController(
    private val postLiveChatMessageUseCase: PostLiveChatMessageUseCase,
    private val messagingTemplate: SimpMessagingTemplate,
) {

    @MessageMapping("/live-chat/{roomId}/send")
    fun send(@DestinationVariable roomId: Long, principal: Principal, request: LiveChatSendMessageRequest) {
        val senderId = principal.name.toLong()
        val result = postLiveChatMessageUseCase.execute(roomId, senderId, request.body)
        messagingTemplate.convertAndSend(
            "/topic/live-chat/$roomId",
            LiveChatMessageBroadcast(id = result.id, roomId = result.roomId, senderId = result.senderId, body = result.body, createdAt = result.createdAt),
        )
    }
}
