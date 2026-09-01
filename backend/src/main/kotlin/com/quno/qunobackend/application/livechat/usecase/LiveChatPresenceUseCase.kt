package com.quno.qunobackend.application.livechat.usecase

import com.quno.qunobackend.domain.livechat.LiveChatPresenceTracker
import org.springframework.stereotype.Service

/** Thin wrapper around [LiveChatPresenceTracker] — called directly by
 * infrastructure/websocket/PresenceEventListener on STOMP subscribe/unsubscribe, not by any REST
 * controller. Kept as one use case (three small operations) rather than three files, since none
 * of them do anything beyond delegating to the port. */
@Service
class LiveChatPresenceUseCase(
    private val liveChatPresenceTracker: LiveChatPresenceTracker,
) {
    fun join(questionId: Long, sessionId: String) = liveChatPresenceTracker.join(questionId, sessionId)
    fun leave(questionId: Long, sessionId: String) = liveChatPresenceTracker.leave(questionId, sessionId)
    fun countViewers(questionId: Long): Long = liveChatPresenceTracker.countViewers(questionId)
}
