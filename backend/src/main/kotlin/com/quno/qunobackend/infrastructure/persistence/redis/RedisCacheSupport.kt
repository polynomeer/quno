package com.quno.qunobackend.infrastructure.persistence.redis

import org.slf4j.LoggerFactory
import org.springframework.dao.DataAccessException
import org.springframework.data.redis.core.StringRedisTemplate
import java.time.Duration

private val logger = LoggerFactory.getLogger("RedisCacheSupport")

/**
 * Cache-aside read/write that degrades to "cache miss"/no-op instead of failing the request when
 * Redis is unreachable — a cache is an optimization on top of the DB, never a hard dependency
 * (장애 시나리오 A4, ADR-0056). [OrganizationRepositoryAdapter]/[DashboardRepositoryAdapter] both
 * treat the DB as the source of truth, unlike [com.quno.qunobackend.infrastructure.livechat.RedisLiveChatPresenceTracker]
 * where Redis genuinely is the source of truth and is left as a hard dependency.
 */
fun StringRedisTemplate.safeCacheGet(key: String): String? = try {
    opsForValue().get(key)
} catch (e: DataAccessException) {
    logger.warn("Redis unavailable, skipping cache read for key={}", key, e)
    null
}

fun StringRedisTemplate.safeCacheSet(key: String, value: String, ttl: Duration) {
    try {
        opsForValue().set(key, value, ttl)
    } catch (e: DataAccessException) {
        logger.warn("Redis unavailable, skipping cache write for key={}", key, e)
    }
}
