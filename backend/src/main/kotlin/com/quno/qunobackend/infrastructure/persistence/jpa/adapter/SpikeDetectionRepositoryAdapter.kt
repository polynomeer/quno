package com.quno.qunobackend.infrastructure.persistence.jpa.adapter

import com.quno.qunobackend.domain.qunobot.SpikeDetectionRepository
import com.quno.qunobackend.domain.qunobot.TagSpike
import com.quno.qunobackend.infrastructure.persistence.jpa.repository.SpikeDetectionJpaRepository
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.stereotype.Component
import tools.jackson.databind.ObjectMapper
import java.time.Duration

/** Cache-aside, same pattern as DashboardRepositoryAdapter (ADR-0009) — same result for every caller. */
@Component
class SpikeDetectionRepositoryAdapter(
    private val jpaRepository: SpikeDetectionJpaRepository,
    private val redisTemplate: StringRedisTemplate,
    private val objectMapper: ObjectMapper,
) : SpikeDetectionRepository {

    override fun findSpikingTags(limit: Int): List<TagSpike> {
        val key = "$SPIKING_TAGS_KEY:$limit"
        redisTemplate.opsForValue().get(key)?.let { cached ->
            return objectMapper.readValue(cached, Array<TagSpike>::class.java).toList()
        }

        val result = jpaRepository.findSpikingTags(limit).map {
            TagSpike(
                id = it.getId(),
                name = it.getName(),
                slug = it.getSlug(),
                recentCount = it.getRecentCount(),
                baselineAveragePerDay = it.getBaselineAveragePerDay(),
                spikeRatio = it.getSpikeRatio(),
            )
        }
        redisTemplate.opsForValue().set(key, objectMapper.writeValueAsString(result), CACHE_TTL)
        return result
    }

    companion object {
        private const val SPIKING_TAGS_KEY = "qunobot:tag-spikes"
        private val CACHE_TTL = Duration.ofSeconds(60)
    }
}
