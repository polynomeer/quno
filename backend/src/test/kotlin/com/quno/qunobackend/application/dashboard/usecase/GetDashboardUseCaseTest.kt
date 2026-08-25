package com.quno.qunobackend.application.dashboard.usecase

import com.quno.qunobackend.application.answer.usecase.InMemoryAnswerRepository
import com.quno.qunobackend.application.cluster.usecase.InMemoryQuestionClusterRepository
import com.quno.qunobackend.application.common.QuestionSummaryHydrator
import com.quno.qunobackend.application.flow.usecase.GetActivityFeedUseCase
import com.quno.qunobackend.application.flow.usecase.InMemoryFlowRepository
import com.quno.qunobackend.application.notification.usecase.InMemoryNotificationRepository
import com.quno.qunobackend.application.notification.usecase.ListMyNotificationsUseCase
import com.quno.qunobackend.application.question.dto.CreateQuestionCommand
import com.quno.qunobackend.application.question.usecase.CreateQuestionUseCase
import com.quno.qunobackend.application.question.usecase.InMemoryQuestionRepository
import com.quno.qunobackend.application.question.usecase.InMemoryQuestionVersionRepository
import com.quno.qunobackend.application.qunobot.usecase.InMemorySpikeDetectionRepository
import com.quno.qunobackend.application.recommendation.usecase.InMemoryRecommendationRepository
import com.quno.qunobackend.application.recommendation.usecase.RecommendQuestionsUseCase
import com.quno.qunobackend.application.tag.usecase.InMemoryQuestionTagRepository
import com.quno.qunobackend.application.tag.usecase.InMemoryTagRepository
import com.quno.qunobackend.domain.dashboard.TagTrend
import com.quno.qunobackend.domain.notification.Notification
import com.quno.qunobackend.domain.qunobot.TagSpike
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class GetDashboardUseCaseTest {
    private val questionRepository = InMemoryQuestionRepository()
    private val tagRepository = InMemoryTagRepository()
    private val questionTagRepository = InMemoryQuestionTagRepository(tagRepository)
    private val createQuestionUseCase = CreateQuestionUseCase(
        questionRepository, InMemoryQuestionVersionRepository(), tagRepository, questionTagRepository,
    )
    private val dashboardRepository = InMemoryDashboardRepository()
    private val notificationRepository = InMemoryNotificationRepository()
    private val recommendationRepository = InMemoryRecommendationRepository()
    private val hydrator = QuestionSummaryHydrator(questionRepository, questionTagRepository)
    private val flowRepository = InMemoryFlowRepository()
    private val spikeDetectionRepository = InMemorySpikeDetectionRepository()
    private val getActivityFeedUseCase = GetActivityFeedUseCase(
        dashboardRepository,
        spikeDetectionRepository,
        flowRepository,
        questionRepository,
        InMemoryAnswerRepository(),
        InMemoryQuestionClusterRepository(),
    )
    private val useCase = GetDashboardUseCase(
        dashboardRepository,
        ListMyNotificationsUseCase(notificationRepository),
        RecommendQuestionsUseCase(recommendationRepository, hydrator),
        hydrator,
        getActivityFeedUseCase,
        flowRepository,
        spikeDetectionRepository,
    )

    @Test
    fun `assembles all sections from their respective sources`() {
        val popular = createQuestionUseCase.execute(
            CreateQuestionCommand(1L, "Popular question", "body", null, null, tagNames = listOf("redis")),
        ).id
        dashboardRepository.popularQuestionIds = listOf(popular)

        notificationRepository.save(Notification.create(userId = 10L, type = "NEW_ANSWER", questionId = popular, answerId = 1L, payload = "{}"))

        val recommended = createQuestionUseCase.execute(
            CreateQuestionCommand(2L, "Recommended question", "body", null, null),
        ).id
        recommendationRepository.recommendationsByUser = mapOf(10L to listOf(recommended))

        dashboardRepository.trendingTags = listOf(TagTrend(id = 1L, name = "redis", slug = "redis", questionCount = 5))

        val resolvedToday = createQuestionUseCase.execute(
            CreateQuestionCommand(3L, "Resolved today", "body", null, null),
        ).id
        dashboardRepository.resolvedTodayQuestionIds = listOf(resolvedToday)

        val reopened = createQuestionUseCase.execute(
            CreateQuestionCommand(4L, "Reopened question", "body", null, null),
        ).id
        flowRepository.reopenedQuestionIds = listOf(reopened)

        spikeDetectionRepository.spikingTags = listOf(TagSpike(1L, "redis", "redis", recentCount = 9, baselineAveragePerDay = 1.0, spikeRatio = 9.0))

        val result = useCase.execute(userId = 10L)

        assertEquals(listOf(popular), result.popularQuestions.map { it.id })
        assertEquals(1, result.wardUpdates.size)
        assertEquals(listOf(recommended), result.followingTagsFeed.map { it.id })
        assertEquals(listOf("redis"), result.trendingTags.map { it.name })
        assertEquals(listOf(resolvedToday), result.resolvedToday.map { it.id })
        assertEquals(listOf(reopened), result.reopenedKnowledge.map { it.id })
        assertEquals(listOf("redis"), result.trendingErrors.map { it.name })
        // a spike outranks mere popularity for the headline
        assertEquals("redis 관련 질문이 평소보다 9.0배 늘었습니다", result.headline?.text)
    }

    @Test
    fun `falls back to the top popular question for the headline when there is no spike`() {
        val popular = createQuestionUseCase.execute(
            CreateQuestionCommand(1L, "Popular question", "body", null, null),
        ).id
        dashboardRepository.popularQuestionIds = listOf(popular)

        val result = useCase.execute(userId = 10L)

        assertEquals(popular, result.headline?.questionId)
    }

    @Test
    fun `headline is null when there is no signal at all`() {
        val result = useCase.execute(userId = 10L)

        assertNull(result.headline)
    }
}
