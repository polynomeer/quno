package com.quno.qunobackend.infrastructure.livechat

import com.quno.qunobackend.domain.livechat.LiveChatPresenceTracker
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.stereotype.Component

/** A Redis Set per question, membership = session ids currently viewing it (Phase 24, ADR-0036).
 * No TTL — membership is maintained explicitly on STOMP subscribe/unsubscribe/disconnect (see
 * infrastructure/websocket/PresenceEventListener), not by expiry. Ephemeral by design: safe to
 * lose on a Redis restart, just resets viewer counts to zero. */
@Component
class RedisLiveChatPresenceTracker(
    private val redisTemplate: StringRedisTemplate,
) : LiveChatPresenceTracker {

    override fun join(questionId: Long, sessionId: String) {
        redisTemplate.opsForSet().add(key(questionId), sessionId)
    }

    override fun leave(questionId: Long, sessionId: String) {
        redisTemplate.opsForSet().remove(key(questionId), sessionId)
    }

    override fun countViewers(questionId: Long): Long = redisTemplate.opsForSet().size(key(questionId)) ?: 0

    private fun key(questionId: Long) = "livechat:presence:$questionId"
}
