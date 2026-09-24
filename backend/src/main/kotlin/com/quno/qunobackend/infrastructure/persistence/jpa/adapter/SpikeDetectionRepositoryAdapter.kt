package com.quno.qunobackend.infrastructure.persistence.jpa.adapter

import com.quno.qunobackend.domain.qunobot.SpikeDetectionRepository
import com.quno.qunobackend.domain.qunobot.TagSpike
import com.quno.qunobackend.infrastructure.persistence.jpa.repository.SpikeDetectionJpaRepository
import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Component

/** Cache-aside, same pattern as DashboardRepositoryAdapter (ADR-0009) — same result for every
 * caller. `@Cacheable`(CacheConfig)이 TTL·직렬화·Redis 장애 시 DB 폴백을 전부 대신 처리한다 —
 * 예전엔 이 클래스가 직접 `safeCacheGet`/`safeCacheSet`으로 구현했었다(ADR-0056). */
@Component
class SpikeDetectionRepositoryAdapter(
    private val jpaRepository: SpikeDetectionJpaRepository,
) : SpikeDetectionRepository {

    @Cacheable(cacheNames = ["qunobot-tag-spikes"], key = "#limit")
    override fun findSpikingTags(limit: Int): List<TagSpike> = jpaRepository.findSpikingTags(limit).map {
        TagSpike(
            id = it.getId(),
            name = it.getName(),
            slug = it.getSlug(),
            recentCount = it.getRecentCount(),
            baselineAveragePerDay = it.getBaselineAveragePerDay(),
            spikeRatio = it.getSpikeRatio(),
        )
    }
}
