package com.quno.qunobackend.infrastructure.persistence.jpa.adapter

import com.quno.qunobackend.domain.dashboard.DashboardRepository
import com.quno.qunobackend.domain.dashboard.TagTrend
import com.quno.qunobackend.infrastructure.persistence.jpa.repository.DashboardJpaRepository
import org.springframework.stereotype.Component

@Component
class DashboardRepositoryAdapter(
    private val jpaRepository: DashboardJpaRepository,
) : DashboardRepository {

    override fun findPopularQuestionIds(limit: Int): List<Long> = jpaRepository.findPopularQuestionIds(limit)

    override fun findTrendingTags(limit: Int): List<TagTrend> =
        jpaRepository.findTrendingTags(limit).map { TagTrend(it.getId(), it.getName(), it.getSlug(), it.getQuestionCount()) }
}
