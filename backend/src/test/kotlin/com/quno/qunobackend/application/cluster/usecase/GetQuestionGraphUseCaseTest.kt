package com.quno.qunobackend.application.cluster.usecase

import com.quno.qunobackend.application.common.QuestionSummaryHydrator
import com.quno.qunobackend.application.question.dto.CreateQuestionCommand
import com.quno.qunobackend.application.question.dto.ForkQuestionCommand
import com.quno.qunobackend.application.question.usecase.CreateQuestionUseCase
import com.quno.qunobackend.application.question.usecase.ForkQuestionUseCase
import com.quno.qunobackend.application.question.usecase.InMemoryQuestionRepository
import com.quno.qunobackend.application.question.usecase.InMemoryQuestionVersionRepository
import com.quno.qunobackend.application.search.usecase.InMemorySearchRepository
import com.quno.qunobackend.application.search.usecase.QuestionSearchUseCase
import com.quno.qunobackend.application.tag.usecase.InMemoryQuestionTagRepository
import com.quno.qunobackend.application.tag.usecase.InMemoryTagRepository
import com.quno.qunobackend.application.vote.usecase.InMemoryVoteRepository
import com.quno.qunobackend.domain.question.QuestionNotFoundException
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

class GetQuestionGraphUseCaseTest {
    private val questionRepository = InMemoryQuestionRepository()
    private val questionVersionRepository = InMemoryQuestionVersionRepository()
    private val tagRepository = InMemoryTagRepository()
    private val questionTagRepository = InMemoryQuestionTagRepository(tagRepository)
    private val questionClusterRepository = InMemoryQuestionClusterRepository()
    private val searchRepository = InMemorySearchRepository()
    private val hydrator = QuestionSummaryHydrator(questionRepository, questionTagRepository, InMemoryVoteRepository())

    private val createQuestionUseCase = CreateQuestionUseCase(questionRepository, questionVersionRepository, tagRepository, questionTagRepository)
    private val forkQuestionUseCase = ForkQuestionUseCase(questionRepository, questionVersionRepository, questionTagRepository)
    private val markQuestionsAsSameProblemUseCase = MarkQuestionsAsSameProblemUseCase(questionRepository, questionClusterRepository)
    private val questionSearchUseCase = QuestionSearchUseCase(searchRepository, questionRepository, hydrator)
    private val getQuestionGraphUseCase = GetQuestionGraphUseCase(questionRepository, hydrator, questionSearchUseCase)

    private fun aQuestion(): Long = createQuestionUseCase.execute(
        CreateQuestionCommand(authorId = 1L, title = "t", body = "body", environment = null, logs = null),
    ).id

    @Test
    fun `combines cluster members, fork lineage, and related questions`() {
        val origin = aQuestion()
        val clusterMate = aQuestion()
        markQuestionsAsSameProblemUseCase.execute(origin, clusterMate)
        val fork = forkQuestionUseCase.execute(ForkQuestionCommand(origin, actorId = 2L)).id
        val related = aQuestion()
        searchRepository.relatedResults = mapOf(origin to listOf(related))

        val result = getQuestionGraphUseCase.execute(origin)

        assertEquals(setOf(clusterMate), result.clusterMembers.map { it.id }.toSet())
        assertNull(result.forkedFrom)
        assertEquals(setOf(fork), result.forks.map { it.id }.toSet())
        assertEquals(listOf(related), result.relatedQuestions.map { it.id })
    }

    @Test
    fun `a forked question's graph shows where it was forked from`() {
        val origin = aQuestion()
        val fork = forkQuestionUseCase.execute(ForkQuestionCommand(origin, actorId = 2L)).id

        val result = getQuestionGraphUseCase.execute(fork)

        assertEquals(origin, result.forkedFrom?.id)
    }

    @Test
    fun `a question with no cluster or forks returns empty lists, not an error`() {
        val questionId = aQuestion()

        val result = getQuestionGraphUseCase.execute(questionId)

        assertEquals(emptyList(), result.clusterMembers)
        assertEquals(emptyList(), result.forks)
        assertNull(result.forkedFrom)
    }

    @Test
    fun `rejects a question that does not exist`() {
        assertFailsWith<QuestionNotFoundException> { getQuestionGraphUseCase.execute(999L) }
    }
}
