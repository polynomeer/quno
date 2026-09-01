package com.quno.qunobackend.infrastructure.websocket

import com.quno.qunobackend.application.user.TokenProvider
import com.quno.qunobackend.domain.user.InvalidTokenException
import org.springframework.messaging.Message
import org.springframework.messaging.MessageChannel
import org.springframework.messaging.support.MessageHeaderAccessor
import org.springframework.messaging.simp.stomp.StompCommand
import org.springframework.messaging.simp.stomp.StompHeaderAccessor
import org.springframework.messaging.support.ChannelInterceptor
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.stereotype.Component

/**
 * Authenticates the STOMP CONNECT frame's `Authorization: Bearer <token>` native header — the
 * WebSocket handshake itself stays unauthenticated (see WebSocketConfig's kdoc), so this is
 * where a bad/missing token actually gets rejected. Sets the resolved user id as the session's
 * Principal, which [LiveChatWebSocketController] and [PresenceEventListener] both read via
 * `Principal.getName()`.
 */
@Component
class StompAuthChannelInterceptor(
    private val tokenProvider: TokenProvider,
) : ChannelInterceptor {

    override fun preSend(message: Message<*>, channel: MessageChannel): Message<*> {
        // StompHeaderAccessor.wrap(message) would create a detached copy — mutating that
        // wouldn't touch the message actually sent downstream. getAccessor(...) instead returns
        // the mutable accessor already embedded in the message, so setting `.user` here is what
        // makes it visible to every later frame in this STOMP session (e.g. the SEND handler in
        // LiveChatWebSocketController reading `Principal`).
        val accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor::class.java) ?: return message
        if (accessor.command != StompCommand.CONNECT) return message

        val header = accessor.getFirstNativeHeader("Authorization")
        require(header != null && header.startsWith(BEARER_PREFIX)) { "Missing Authorization header" }
        val token = header.removePrefix(BEARER_PREFIX).trim()
        val userId = try {
            tokenProvider.validateAccessToken(token)
        } catch (e: InvalidTokenException) {
            throw IllegalArgumentException("Invalid access token", e)
        }

        accessor.user = UsernamePasswordAuthenticationToken(userId.toString(), null, emptyList())
        return message
    }

    companion object {
        private const val BEARER_PREFIX = "Bearer "
    }
}
