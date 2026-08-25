package com.quno.qunobackend.application.dashboard.usecase

import com.quno.qunobackend.domain.dashboard.DashboardRepository
import com.quno.qunobackend.domain.dashboard.TagTrend

class InMemoryDashboardRepository : DashboardRepository {
    var popularQuestionIds: List<Long> = emptyList()
    var trendingTags: List<TagTrend> = emptyList()
    var resolvedTodayQuestionIds: List<Long> = emptyList()

    override fun findPopularQuestionIds(limit: Int): List<Long> = popularQuestionIds.take(limit)

    override fun findTrendingTags(limit: Int): List<TagTrend> = trendingTags.take(limit)

    override fun findResolvedTodayQuestionIds(limit: Int): List<Long> = resolvedTodayQuestionIds.take(limit)
}
