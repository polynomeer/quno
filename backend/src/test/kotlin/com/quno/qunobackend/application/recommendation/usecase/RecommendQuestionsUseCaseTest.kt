package com.quno.qunobackend.application.recommendation.usecase

import com.quno.qunobackend.application.common.QuestionSummaryHydrator
import com.quno.qunobackend.application.question.dto.CreateQuestionCommand
import com.quno.qunobackend.application.question.usecase.CreateQuestionUseCase
import com.quno.qunobackend.application.question.usecase.InMemoryQuestionRepository
import com.quno.qunobackend.application.question.usecase.InMemoryQuestionVersionRepository
import com.quno.qunobackend.application.tag.usecase.InMemoryQuestionTagRepository
import com.quno.qunobackend.application.tag.usecase.InMemoryTagRepository
import com.quno.qunobackend.application.vote.usecase.InMemoryVoteRepository
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import org.junit.jupiter.api.Test

class RecommendQuestionsUseCaseTest {
    private val questionRepository = InMemoryQuestionRepository()
    private val tagRepository = InMemoryTagRepository()
    private val questionTagRepository = InMemoryQuestionTagRepository(tagRepository)
    private val createUseCase = CreateQuestionUseCase(
        questionRepository, InMemoryQuestionVersionRepository(), tagRepository, questionTagRepository,
    )
    private val recommendationRepository = InMemoryRecommendationRepository()
    private val useCase = RecommendQuestionsUseCase(
        recommendationRepository,
        QuestionSummaryHydrator(questionRepository, questionTagRepository, InMemoryVoteRepository()),
    )

    @Test
    fun `hydrates the repository's ranked ids for the requesting user`() {
        val q1 = createUseCase.execute(
            CreateQuestionCommand(authorId = 2L, title = "Redis timeout", body = "body", environment = null, logs = null, tagNames = listOf("redis")),
        ).id
        recommendationRepository.recommendationsByUser = mapOf(1L to listOf(q1))

        val result = useCase.execute(userId = 1L)

        assertEquals(listOf(q1), result.map { it.id })
        assertEquals(listOf("redis"), result.single().tags)
    }

    @Test
    fun `returns nothing for a user with no recommendations`() {
        assertTrue(useCase.execute(userId = 999L).isEmpty())
    }
}
