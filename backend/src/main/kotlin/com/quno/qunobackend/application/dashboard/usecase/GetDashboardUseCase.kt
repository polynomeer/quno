package com.quno.qunobackend.application.dashboard.usecase

import com.quno.qunobackend.application.common.QuestionSummaryHydrator
import com.quno.qunobackend.application.dashboard.dto.DashboardHeadline
import com.quno.qunobackend.application.dashboard.dto.DashboardResult
import com.quno.qunobackend.application.dashboard.dto.TagTrendResult
import com.quno.qunobackend.application.flow.usecase.GetActivityFeedUseCase
import com.quno.qunobackend.application.notification.usecase.ListMyNotificationsUseCase
import com.quno.qunobackend.application.recommendation.usecase.RecommendQuestionsUseCase
import com.quno.qunobackend.domain.dashboard.DashboardRepository
import com.quno.qunobackend.domain.flow.FlowCardType
import com.quno.qunobackend.domain.flow.FlowRepository
import com.quno.qunobackend.domain.qunobot.SpikeDetectionRepository
import org.springframework.stereotype.Service

/**
 * Combines read models into the "newspaper front page" home screen from
 * docs/product/vision.md — each section reuses an existing use case/port rather than
 * introducing dashboard-specific business logic. Extended in PLAN.md 10.2 ("고급 Daily
 * Dashboard") with a headline, today's resolved questions, reopened knowledge, and trending
 * errors — all reusing signals already built for Spike Detection (Phase 8) and Quno Flow
 * (Phase 10.1/10.3).
 */
@Service
class GetDashboardUseCase(
    private val dashboardRepository: DashboardRepository,
    private val listMyNotificationsUseCase: ListMyNotificationsUseCase,
    private val recommendQuestionsUseCase: RecommendQuestionsUseCase,
    private val hydrator: QuestionSummaryHydrator,
    private val getActivityFeedUseCase: GetActivityFeedUseCase,
    private val flowRepository: FlowRepository,
    private val spikeDetectionRepository: SpikeDetectionRepository,
) {
    fun execute(userId: Long): DashboardResult {
        val popularQuestions = hydrator.hydrate(dashboardRepository.findPopularQuestionIds(POPULAR_QUESTIONS_LIMIT))
        val wardUpdates = listMyNotificationsUseCase.execute(userId).take(WARD_UPDATES_LIMIT)
        val followingTagsFeed = recommendQuestionsUseCase.execute(userId, FOLLOWING_FEED_LIMIT)
        val trendingTags = dashboardRepository.findTrendingTags(TRENDING_TAGS_LIMIT).map {
            TagTrendResult(id = it.id, name = it.name, slug = it.slug, questionCount = it.questionCount)
        }

        // Headline: the single most attention-worthy signal — a tag spike outranks mere
        // popularity, since "something unusual is happening" is more newsworthy than "this is
        // popular as usual". Reuses Quno Flow's own card assembly instead of duplicating it.
        val headlineCards = getActivityFeedUseCase.execute(1)
        val headline = (headlineCards.firstOrNull { it.type == FlowCardType.TAG_SPIKE }
            ?: headlineCards.firstOrNull { it.type == FlowCardType.POPULAR_QUESTION })
            ?.let { DashboardHeadline(text = it.headline, questionId = it.questionId) }

        val resolvedToday = hydrator.hydrate(dashboardRepository.findResolvedTodayQuestionIds(RESOLVED_TODAY_LIMIT))
        val reopenedKnowledge = hydrator.hydrate(flowRepository.findRecentlyReopenedQuestionIds(REOPENED_LIMIT))
        val trendingErrors = spikeDetectionRepository.findSpikingTags(TRENDING_ERRORS_LIMIT)

        return DashboardResult(
            popularQuestions = popularQuestions,
            wardUpdates = wardUpdates,
            followingTagsFeed = followingTagsFeed,
            trendingTags = trendingTags,
            headline = headline,
            resolvedToday = resolvedToday,
            reopenedKnowledge = reopenedKnowledge,
            trendingErrors = trendingErrors,
        )
    }

    companion object {
        private const val POPULAR_QUESTIONS_LIMIT = 5
        private const val WARD_UPDATES_LIMIT = 5
        private const val FOLLOWING_FEED_LIMIT = 10
        private const val TRENDING_TAGS_LIMIT = 10
        private const val RESOLVED_TODAY_LIMIT = 5
        private const val REOPENED_LIMIT = 5
        private const val TRENDING_ERRORS_LIMIT = 5
    }
}
