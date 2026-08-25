package com.quno.qunobackend.infrastructure.persistence.jpa.adapter

import com.quno.qunobackend.domain.dashboard.DashboardRepository
import com.quno.qunobackend.domain.dashboard.TagTrend
import com.quno.qunobackend.infrastructure.persistence.jpa.repository.DashboardJpaRepository
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.stereotype.Component
import tools.jackson.databind.ObjectMapper
import java.time.Duration

/**
 * Cache-aside on top of the native ranking queries — see docs/architecture/api-design.md
 * "Redis 캐시 (Phase 3.4)". Only the two sections that are the *same for every user*
 * (popular questions, trending tags) are cached; per-user sections (Ward updates, the
 * following-tags feed) are not, since staleness there reads as a correctness bug, not
 * just a slightly-behind trend.
 */
@Component
class DashboardRepositoryAdapter(
    private val jpaRepository: DashboardJpaRepository,
    private val redisTemplate: StringRedisTemplate,
    private val objectMapper: ObjectMapper,
) : DashboardRepository {

    override fun findPopularQuestionIds(limit: Int): List<Long> {
        val key = "$POPULAR_QUESTIONS_KEY:$limit"
        redisTemplate.opsForValue().get(key)?.let { cached ->
            return objectMapper.readValue(cached, Array<Long>::class.java).toList()
        }

        val result = jpaRepository.findPopularQuestionIds(limit)
        redisTemplate.opsForValue().set(key, objectMapper.writeValueAsString(result), CACHE_TTL)
        return result
    }

    override fun findTrendingTags(limit: Int): List<TagTrend> {
        val key = "$TRENDING_TAGS_KEY:$limit"
        redisTemplate.opsForValue().get(key)?.let { cached ->
            return objectMapper.readValue(cached, Array<TagTrend>::class.java).toList()
        }

        val result = jpaRepository.findTrendingTags(limit)
            .map { TagTrend(it.getId(), it.getName(), it.getSlug(), it.getQuestionCount()) }
        redisTemplate.opsForValue().set(key, objectMapper.writeValueAsString(result), CACHE_TTL)
        return result
    }

    companion object {
        private const val POPULAR_QUESTIONS_KEY = "dashboard:popular-questions"
        private const val TRENDING_TAGS_KEY = "dashboard:trending-tags"
        private val CACHE_TTL = Duration.ofSeconds(60)
    }
}
