package com.quno.qunobackend.infrastructure.question

import com.quno.qunobackend.domain.question.QuestionViewDeduplicator
import org.slf4j.LoggerFactory
import org.springframework.dao.DataAccessException
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.stereotype.Component
import java.time.Duration

/**
 * One Redis key per (question, viewer) with a TTL, set with SET NX — the key existing means "already
 * counted in this window" (ADR-0063). Viewer keys are opaque (user id, or a hash of IP + User-Agent
 * built in the controller), so no raw IP is ever stored.
 *
 * Unlike live-chat presence, Redis is not the source of truth here, so an outage degrades to
 * "count every view" instead of failing the request (ADR-0056's policy).
 */
@Component
class RedisQuestionViewDeduplicator(
    private val redisTemplate: StringRedisTemplate,
) : QuestionViewDeduplicator {

    override fun isFirstView(questionId: Long, viewerKey: String): Boolean = try {
        redisTemplate.opsForValue().setIfAbsent("question:view:$questionId:$viewerKey", "1", WINDOW) ?: true
    } catch (e: DataAccessException) {
        logger.warn("Redis unavailable, counting question view without dedup questionId={}", questionId, e)
        true
    }

    companion object {
        val WINDOW: Duration = Duration.ofMinutes(30)
        private val logger = LoggerFactory.getLogger(RedisQuestionViewDeduplicator::class.java)
    }
}
