package com.quno.qunobackend.application.dashboard.usecase

import com.quno.qunobackend.application.common.QuestionSummaryHydrator
import com.quno.qunobackend.application.notification.usecase.InMemoryNotificationRepository
import com.quno.qunobackend.application.notification.usecase.ListMyNotificationsUseCase
import com.quno.qunobackend.application.question.dto.CreateQuestionCommand
import com.quno.qunobackend.application.question.usecase.CreateQuestionUseCase
import com.quno.qunobackend.application.question.usecase.InMemoryQuestionRepository
import com.quno.qunobackend.application.question.usecase.InMemoryQuestionVersionRepository
import com.quno.qunobackend.application.recommendation.usecase.InMemoryRecommendationRepository
import com.quno.qunobackend.application.recommendation.usecase.RecommendQuestionsUseCase
import com.quno.qunobackend.application.tag.usecase.InMemoryQuestionTagRepository
import com.quno.qunobackend.application.tag.usecase.InMemoryTagRepository
import com.quno.qunobackend.domain.dashboard.TagTrend
import com.quno.qunobackend.domain.notification.Notification
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

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
    private val useCase = GetDashboardUseCase(
        dashboardRepository,
        ListMyNotificationsUseCase(notificationRepository),
        RecommendQuestionsUseCase(recommendationRepository, hydrator),
        hydrator,
    )

    @Test
    fun `assembles all four sections from their respective sources`() {
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

        val result = useCase.execute(userId = 10L)

        assertEquals(listOf(popular), result.popularQuestions.map { it.id })
        assertEquals(1, result.wardUpdates.size)
        assertEquals(listOf(recommended), result.followingTagsFeed.map { it.id })
        assertEquals(listOf("redis"), result.trendingTags.map { it.name })
    }
}
