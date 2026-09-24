package com.quno.qunobackend.infrastructure.persistence.jpa.adapter

import com.quno.qunobackend.domain.dashboard.DashboardRepository
import com.quno.qunobackend.domain.dashboard.TagTrend
import com.quno.qunobackend.infrastructure.persistence.jpa.repository.DashboardJpaRepository
import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Component

/**
 * Cache-aside on top of the native ranking queries — see docs/architecture/api-design.md
 * "Redis 캐시 (Phase 3.4)". Only the two sections that are the *same for every user*
 * (popular questions, trending tags) are cached; per-user sections (Ward updates, the
 * following-tags feed) are not, since staleness there reads as a correctness bug, not
 * just a slightly-behind trend. `@Cacheable`(CacheConfig)이 TTL·직렬화·Redis 장애 시 DB 폴백을
 * 전부 대신 처리한다 — 예전엔 이 클래스가 직접 `safeCacheGet`/`safeCacheSet`으로 구현했었다
 * (ADR-0056), 지금은 선언적 캐싱으로 옮겨 그 로직이 여기 남아있지 않다.
 */
@Component
class DashboardRepositoryAdapter(
    private val jpaRepository: DashboardJpaRepository,
) : DashboardRepository {

    @Cacheable(cacheNames = ["dashboard-popular-questions"], key = "#limit")
    override fun findPopularQuestionIds(limit: Int): List<Long> = jpaRepository.findPopularQuestionIds(limit)

    @Cacheable(cacheNames = ["dashboard-trending-tags"], key = "#limit")
    override fun findTrendingTags(limit: Int): List<TagTrend> = jpaRepository.findTrendingTags(limit).map { TagTrend(it.getId(), it.getName(), it.getSlug(), it.getQuestionCount()) }

    /** Not cached — a simple indexed query, unlike the two aggregate rankings above. */
    override fun findResolvedTodayQuestionIds(limit: Int): List<Long> = jpaRepository.findResolvedTodayQuestionIds(limit)
}
