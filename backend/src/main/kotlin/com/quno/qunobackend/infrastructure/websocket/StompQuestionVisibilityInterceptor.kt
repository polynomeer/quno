package com.quno.qunobackend.infrastructure.websocket

import com.quno.qunobackend.application.common.QuestionAccess
import com.quno.qunobackend.application.common.QuestionScopedResource
import org.springframework.messaging.Message
import org.springframework.messaging.MessageChannel
import org.springframework.messaging.simp.stomp.StompCommand
import org.springframework.messaging.simp.stomp.StompHeaderAccessor
import org.springframework.messaging.support.ChannelInterceptor
import org.springframework.messaging.support.MessageHeaderAccessor
import org.springframework.stereotype.Component

/**
 * The STOMP side of the request gate (Phase 79, ADR-0085). SUBSCRIBE and SEND frames whose
 * destination belongs to a question — its presence topic, a live-chat room — are refused when the
 * question is hidden from the connected user. Any other `/topic` destination is refused too: every
 * topic today belongs to a question, and a new one should be added here on purpose.
 */
@Component
class StompQuestionVisibilityInterceptor(
    private val access: QuestionAccess,
) : ChannelInterceptor {

    override fun preSend(message: Message<*>, channel: MessageChannel): Message<*> {
        val accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor::class.java) ?: return message
        if (accessor.command != StompCommand.SUBSCRIBE && accessor.command != StompCommand.SEND) return message
        val destination = accessor.destination ?: return message
        val target = match(destination)
        if (target == null) {
            require(!destination.startsWith("/topic/")) { "Unknown destination" }
            return message
        }
        val viewer = access.viewer(accessor.user?.name?.toLongOrNull())
        val (resource, id) = target
        // Same message for hidden and missing, so a frame can't be used to probe.
        require(!access.isHidden(resource, id, viewer)) { "Unknown destination" }
        return message
    }

    companion object {
        private val PATTERNS = listOf(
            Regex("""^/topic/questions/(\d+)/presence$""") to QuestionScopedResource.QUESTION,
            Regex("""^/topic/live-chat/(\d+)$""") to QuestionScopedResource.LIVE_CHAT_ROOM,
            Regex("""^/app/live-chat/(\d+)/send$""") to QuestionScopedResource.LIVE_CHAT_ROOM,
        )

        fun match(destination: String): Pair<QuestionScopedResource, Long>? = PATTERNS.firstNotNullOfOrNull { (regex, resource) ->
            regex.matchEntire(destination)?.groupValues?.get(1)?.toLongOrNull()?.let { resource to it }
        }
    }
}
