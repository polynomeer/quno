package com.quno.qunobackend.application.cluster.usecase

import com.quno.qunobackend.application.common.QuestionSummaryHydrator
import com.quno.qunobackend.application.question.dto.CreateQuestionCommand
import com.quno.qunobackend.application.question.usecase.CreateQuestionUseCase
import com.quno.qunobackend.application.question.usecase.InMemoryQuestionRepository
import com.quno.qunobackend.application.question.usecase.InMemoryQuestionVersionRepository
import com.quno.qunobackend.application.tag.usecase.InMemoryQuestionTagRepository
import com.quno.qunobackend.application.tag.usecase.InMemoryTagRepository
import com.quno.qunobackend.domain.cluster.ClusterNotFoundException
import com.quno.qunobackend.domain.question.QuestionNotFoundException
import com.quno.qunobackend.domain.cluster.QuestionNotInAnyClusterException
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class GetClusterUseCaseTest {
    private val questionRepository = InMemoryQuestionRepository()
    private val questionVersionRepository = InMemoryQuestionVersionRepository()
    private val tagRepository = InMemoryTagRepository()
    private val questionTagRepository = InMemoryQuestionTagRepository(tagRepository)
    private val questionClusterRepository = InMemoryQuestionClusterRepository()

    private val createQuestionUseCase = CreateQuestionUseCase(
        questionRepository, questionVersionRepository, tagRepository, questionTagRepository,
    )
    private val markQuestionsAsSameProblemUseCase = MarkQuestionsAsSameProblemUseCase(questionRepository, questionClusterRepository)
    private val useCase = GetClusterUseCase(
        questionClusterRepository, questionRepository, QuestionSummaryHydrator(questionRepository, questionTagRepository),
    )

    private fun questionAskedBy(authorId: Long): Long =
        createQuestionUseCase.execute(
            CreateQuestionCommand(authorId = authorId, title = "t", body = "body", environment = null, logs = null),
        ).id

    @Test
    fun `returns the cluster's member questions`() {
        val a = questionAskedBy(1L)
        val b = questionAskedBy(2L)
        val marked = markQuestionsAsSameProblemUseCase.execute(a, b)

        val result = useCase.execute(marked.clusterId)

        assertEquals(setOf(a, b), result.members.map { it.id }.toSet())
    }

    @Test
    fun `looking up a cluster by an unclustered question fails`() {
        val a = questionAskedBy(1L)

        assertFailsWith<QuestionNotInAnyClusterException> { useCase.executeForQuestion(a) }
    }

    @Test
    fun `looking up a cluster for a question that does not exist fails`() {
        assertFailsWith<QuestionNotFoundException> { useCase.executeForQuestion(999L) }
    }

    @Test
    fun `looking up a cluster id that does not exist fails`() {
        assertFailsWith<ClusterNotFoundException> { useCase.execute(999L) }
    }

    @Test
    fun `looking up a cluster via the owning question returns the same cluster`() {
        val a = questionAskedBy(1L)
        val b = questionAskedBy(2L)
        markQuestionsAsSameProblemUseCase.execute(a, b)

        val result = useCase.executeForQuestion(a)

        assertEquals(setOf(a, b), result.members.map { it.id }.toSet())
    }
}
