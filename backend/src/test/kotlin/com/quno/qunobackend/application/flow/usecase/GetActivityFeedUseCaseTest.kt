package com.quno.qunobackend.application.flow.usecase

import com.quno.qunobackend.application.answer.dto.AcceptAnswerCommand
import com.quno.qunobackend.application.answer.dto.WriteAnswerCommand
import com.quno.qunobackend.application.answer.usecase.AcceptAnswerUseCase
import com.quno.qunobackend.application.answer.usecase.InMemoryAnswerRepository
import com.quno.qunobackend.application.answer.usecase.WriteAnswerUseCase
import com.quno.qunobackend.application.cluster.usecase.InMemoryQuestionClusterRepository
import com.quno.qunobackend.application.cluster.usecase.MarkQuestionsAsSameProblemUseCase
import com.quno.qunobackend.application.common.AnswerResultAssembler
import com.quno.qunobackend.application.common.InMemoryOutboxEventRepository
import com.quno.qunobackend.application.dashboard.usecase.InMemoryDashboardRepository
import com.quno.qunobackend.application.question.dto.CreateQuestionCommand
import com.quno.qunobackend.application.question.usecase.CreateQuestionUseCase
import com.quno.qunobackend.application.question.usecase.InMemoryQuestionRepository
import com.quno.qunobackend.application.question.usecase.InMemoryQuestionVersionRepository
import com.quno.qunobackend.application.qunobot.usecase.InMemorySpikeDetectionRepository
import com.quno.qunobackend.application.tag.usecase.InMemoryQuestionTagRepository
import com.quno.qunobackend.application.tag.usecase.InMemoryTagRepository
import com.quno.qunobackend.application.vote.usecase.InMemoryVoteRepository
import com.quno.qunobackend.domain.flow.FlowCardType
import com.quno.qunobackend.domain.qunobot.TagSpike
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import org.junit.jupiter.api.Test

class GetActivityFeedUseCaseTest {
    private val questionRepository = InMemoryQuestionRepository()
    private val questionVersionRepository = InMemoryQuestionVersionRepository()
    private val tagRepository = InMemoryTagRepository()
    private val answerRepository = InMemoryAnswerRepository()
    private val questionClusterRepository = InMemoryQuestionClusterRepository()
    private val dashboardRepository = InMemoryDashboardRepository()
    private val spikeDetectionRepository = InMemorySpikeDetectionRepository()
    private val flowRepository = InMemoryFlowRepository()
    private val outboxEventRepository = InMemoryOutboxEventRepository()

    private val createQuestionUseCase = CreateQuestionUseCase(
        questionRepository, questionVersionRepository, tagRepository, InMemoryQuestionTagRepository(tagRepository),
    )
    private val writeAnswerUseCase = WriteAnswerUseCase(
        questionRepository, questionVersionRepository, answerRepository, outboxEventRepository,
        AnswerResultAssembler(questionRepository, questionVersionRepository, InMemoryVoteRepository()),
    )
    private val acceptAnswerUseCase = AcceptAnswerUseCase(questionRepository, answerRepository, outboxEventRepository)
    private val markQuestionsAsSameProblemUseCase = MarkQuestionsAsSameProblemUseCase(questionRepository, questionClusterRepository)
    private val useCase = GetActivityFeedUseCase(
        dashboardRepository, spikeDetectionRepository, flowRepository, questionRepository, answerRepository, questionClusterRepository,
    )

    private fun questionAskedBy(authorId: Long): Long =
        createQuestionUseCase.execute(
            CreateQuestionCommand(authorId = authorId, title = "t$authorId", body = "body", environment = null, logs = null),
        ).id

    @Test
    fun `builds a popular question card`() {
        val questionId = questionAskedBy(1L)
        dashboardRepository.popularQuestionIds = listOf(questionId)

        val cards = useCase.execute(limitPerSection = 5)

        val card = cards.single { it.type == FlowCardType.POPULAR_QUESTION }
        assertEquals(questionId, card.questionId)
        assertTrue(card.headline.contains("t1"))
    }

    @Test
    fun `builds a tag spike card`() {
        spikeDetectionRepository.spikingTags = listOf(TagSpike(1L, "redis", "redis", recentCount = 6, baselineAveragePerDay = 1.0, spikeRatio = 6.0))

        val cards = useCase.execute(limitPerSection = 5)

        val card = cards.single { it.type == FlowCardType.TAG_SPIKE }
        assertTrue(card.headline.contains("redis"))
        assertTrue(card.headline.contains("6.0"))
    }

    @Test
    fun `builds a reopened question card`() {
        val questionId = questionAskedBy(1L)
        flowRepository.reopenedQuestionIds = listOf(questionId)

        val cards = useCase.execute(limitPerSection = 5)

        val card = cards.single { it.type == FlowCardType.REOPENED_QUESTION }
        assertEquals(questionId, card.questionId)
    }

    @Test
    fun `builds a cluster super answer card referencing the answer's question title`() {
        val a = questionAskedBy(1L)
        val b = questionAskedBy(2L)
        val marked = markQuestionsAsSameProblemUseCase.execute(a, b)
        val answer = writeAnswerUseCase.execute(WriteAnswerCommand(questionId = a, authorId = 3L, body = "fix"))
        acceptAnswerUseCase.execute(AcceptAnswerCommand(answerId = answer.id, actorId = 1L))
        questionClusterRepository.save(questionClusterRepository.findById(marked.clusterId)!!.designateSuperAnswer(answer.id))
        flowRepository.superAnsweredClusterIds = listOf(marked.clusterId)

        val cards = useCase.execute(limitPerSection = 5)

        val card = cards.single { it.type == FlowCardType.CLUSTER_SUPER_ANSWER }
        assertEquals(marked.clusterId, card.clusterId)
        assertTrue(card.headline.contains("t1"))
    }

    @Test
    fun `returns no cards when there are no signals`() {
        assertEquals(emptyList(), useCase.execute(limitPerSection = 5))
    }
}
