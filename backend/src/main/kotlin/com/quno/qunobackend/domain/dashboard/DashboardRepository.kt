package com.quno.qunobackend.domain.dashboard

/**
 * Port implemented by infrastructure/persistence/jpa/adapter/DashboardRepositoryAdapter.
 *
 * MVP has no page-view tracking, so "popular" is approximated from signals we do have
 * (Watch count, Answer count) plus recency as a tiebreaker — see
 * docs/architecture/api-design.md#라이트-대시보드-phase-32.
 */
interface DashboardRepository {
    /** Ranked by `watch_count * 3 + answer_count * 2`, most recent first as tiebreaker. */
    fun findPopularQuestionIds(limit: Int): List<Long>

    /** Tags on questions created in the last 7 days, ranked by distinct question count. */
    fun findTrendingTags(limit: Int): List<TagTrend>
}
