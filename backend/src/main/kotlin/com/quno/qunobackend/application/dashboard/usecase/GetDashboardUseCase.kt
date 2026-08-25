package com.quno.qunobackend.application.dashboard.usecase

import com.quno.qunobackend.application.common.QuestionSummaryHydrator
import com.quno.qunobackend.application.dashboard.dto.DashboardResult
import com.quno.qunobackend.application.dashboard.dto.TagTrendResult
import com.quno.qunobackend.application.notification.usecase.ListMyNotificationsUseCase
import com.quno.qunobackend.application.recommendation.usecase.RecommendQuestionsUseCase
import com.quno.qunobackend.domain.dashboard.DashboardRepository
import org.springframework.stereotype.Service

/**
 * Combines four read models into the "newspaper front page" home screen from
 * docs/product/vision.md — each section reuses an existing use case/port rather than
 * introducing dashboard-specific business logic.
 */
@Service
class GetDashboardUseCase(
    private val dashboardRepository: DashboardRepository,
    private val listMyNotificationsUseCase: ListMyNotificationsUseCase,
    private val recommendQuestionsUseCase: RecommendQuestionsUseCase,
    private val hydrator: QuestionSummaryHydrator,
) {
    fun execute(userId: Long): DashboardResult {
        val popularQuestions = hydrator.hydrate(dashboardRepository.findPopularQuestionIds(POPULAR_QUESTIONS_LIMIT))
        val wardUpdates = listMyNotificationsUseCase.execute(userId).take(WARD_UPDATES_LIMIT)
        val followingTagsFeed = recommendQuestionsUseCase.execute(userId, FOLLOWING_FEED_LIMIT)
        val trendingTags = dashboardRepository.findTrendingTags(TRENDING_TAGS_LIMIT).map {
            TagTrendResult(id = it.id, name = it.name, slug = it.slug, questionCount = it.questionCount)
        }

        return DashboardResult(
            popularQuestions = popularQuestions,
            wardUpdates = wardUpdates,
            followingTagsFeed = followingTagsFeed,
            trendingTags = trendingTags,
        )
    }

    companion object {
        private const val POPULAR_QUESTIONS_LIMIT = 5
        private const val WARD_UPDATES_LIMIT = 5
        private const val FOLLOWING_FEED_LIMIT = 10
        private const val TRENDING_TAGS_LIMIT = 10
    }
}
