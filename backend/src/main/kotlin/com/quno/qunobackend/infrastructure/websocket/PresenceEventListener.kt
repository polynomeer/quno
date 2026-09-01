package com.quno.qunobackend.infrastructure.websocket

import com.quno.qunobackend.application.livechat.usecase.LiveChatPresenceUseCase
import org.springframework.context.event.EventListener
import org.springframework.messaging.simp.SimpMessagingTemplate
import org.springframework.messaging.simp.stomp.StompHeaderAccessor
import org.springframework.stereotype.Component
import org.springframework.web.socket.messaging.SessionDisconnectEvent
import org.springframework.web.socket.messaging.SessionSubscribeEvent
import org.springframework.web.socket.messaging.SessionUnsubscribeEvent
import java.util.concurrent.ConcurrentHashMap

data class PresenceBroadcast(val viewerCount: Long)

/**
 * Maintains "현재 N명이 이 질문을 보고 있습니다" by watching STOMP subscribe/unsubscribe/
 * disconnect events for `/topic/questions/{id}/presence` (Phase 24, ADR-0036). A client is
 * "viewing" for as long as that subscription is open — no separate heartbeat needed, since STOMP
 * already fires a disconnect event when the underlying WebSocket connection drops.
 *
 * [subscriptions] (sessionId → subscriptionId → questionId) is in-memory and single-instance
 * only — Spring's disconnect event carries no destination, so leaving requires remembering what
 * a session had subscribed to. If this ever runs behind multiple instances, presence would need
 * to move to a shared store keyed the same way (Redis already holds the actual membership; only
 * this reverse-lookup would need to follow).
 */
@Component
class PresenceEventListener(
    private val liveChatPresenceUseCase: LiveChatPresenceUseCase,
    private val messagingTemplate: SimpMessagingTemplate,
) {
    private val subscriptions = ConcurrentHashMap<String, ConcurrentHashMap<String, Long>>()

    @EventListener
    fun onSubscribe(event: SessionSubscribeEvent) {
        val accessor = StompHeaderAccessor.wrap(event.message)
        val questionId = parseQuestionId(accessor.destination) ?: return
        val sessionId = accessor.sessionId ?: return
        val subscriptionId = accessor.subscriptionId ?: return

        subscriptions.computeIfAbsent(sessionId) { ConcurrentHashMap() }[subscriptionId] = questionId
        liveChatPresenceUseCase.join(questionId, sessionId)
        broadcastCount(questionId)
    }

    @EventListener
    fun onUnsubscribe(event: SessionUnsubscribeEvent) {
        val accessor = StompHeaderAccessor.wrap(event.message)
        val sessionId = accessor.sessionId ?: return
        val subscriptionId = accessor.subscriptionId ?: return
        val questionId = subscriptions[sessionId]?.remove(subscriptionId) ?: return

        liveChatPresenceUseCase.leave(questionId, sessionId)
        broadcastCount(questionId)
    }

    @EventListener
    fun onDisconnect(event: SessionDisconnectEvent) {
        val sessionId = event.sessionId
        val questionIds = subscriptions.remove(sessionId)?.values?.toSet() ?: return
        questionIds.forEach { questionId ->
            liveChatPresenceUseCase.leave(questionId, sessionId)
            broadcastCount(questionId)
        }
    }

    private fun broadcastCount(questionId: Long) {
        val count = liveChatPresenceUseCase.countViewers(questionId)
        messagingTemplate.convertAndSend("/topic/questions/$questionId/presence", PresenceBroadcast(count))
    }

    private fun parseQuestionId(destination: String?): Long? =
        destination?.let { QUESTION_PRESENCE_PATTERN.find(it)?.groupValues?.get(1)?.toLongOrNull() }

    companion object {
        private val QUESTION_PRESENCE_PATTERN = Regex("""^/topic/questions/(\d+)/presence$""")
    }
}
