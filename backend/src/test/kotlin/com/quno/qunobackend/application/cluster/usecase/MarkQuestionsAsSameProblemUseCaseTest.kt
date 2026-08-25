package com.quno.qunobackend.application.cluster.usecase

import com.quno.qunobackend.application.question.dto.CreateQuestionCommand
import com.quno.qunobackend.application.question.usecase.CreateQuestionUseCase
import com.quno.qunobackend.application.question.usecase.InMemoryQuestionRepository
import com.quno.qunobackend.application.question.usecase.InMemoryQuestionVersionRepository
import com.quno.qunobackend.application.tag.usecase.InMemoryQuestionTagRepository
import com.quno.qunobackend.application.tag.usecase.InMemoryTagRepository
import com.quno.qunobackend.domain.cluster.CannotClusterWithSelfException
import com.quno.qunobackend.domain.cluster.ClustersAlreadyDistinctException
import com.quno.qunobackend.domain.question.QuestionNotFoundException
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

class MarkQuestionsAsSameProblemUseCaseTest {
    private val questionRepository = InMemoryQuestionRepository()
    private val questionVersionRepository = InMemoryQuestionVersionRepository()
    private val tagRepository = InMemoryTagRepository()
    private val questionClusterRepository = InMemoryQuestionClusterRepository()

    private val createQuestionUseCase = CreateQuestionUseCase(
        questionRepository, questionVersionRepository, tagRepository, InMemoryQuestionTagRepository(tagRepository),
    )
    private val useCase = MarkQuestionsAsSameProblemUseCase(questionRepository, questionClusterRepository)

    private fun questionAskedBy(authorId: Long): Long =
        createQuestionUseCase.execute(
            CreateQuestionCommand(authorId = authorId, title = "t", body = "body", environment = null, logs = null),
        ).id

    @Test
    fun `marking two unclustered questions creates a new cluster containing both`() {
        val a = questionAskedBy(1L)
        val b = questionAskedBy(2L)

        val result = useCase.execute(a, b)

        assertEquals(setOf(a, b), result.memberQuestionIds.toSet())
        assertNull(result.representativeAnswerId)
        assertEquals(result.clusterId, questionRepository.findById(a)!!.clusterId)
        assertEquals(result.clusterId, questionRepository.findById(b)!!.clusterId)
    }

    @Test
    fun `marking an unclustered question against an already clustered one joins the existing cluster`() {
        val a = questionAskedBy(1L)
        val b = questionAskedBy(2L)
        val c = questionAskedBy(3L)
        val first = useCase.execute(a, b)

        val result = useCase.execute(c, a)

        assertEquals(first.clusterId, result.clusterId)
        assertEquals(setOf(a, b, c), result.memberQuestionIds.toSet())
    }

    @Test
    fun `marking two questions already in the same cluster is a no-op`() {
        val a = questionAskedBy(1L)
        val b = questionAskedBy(2L)
        val first = useCase.execute(a, b)

        val result = useCase.execute(a, b)

        assertEquals(first.clusterId, result.clusterId)
        assertEquals(setOf(a, b), result.memberQuestionIds.toSet())
    }

    @Test
    fun `rejects marking two questions that already belong to different clusters`() {
        val a = questionAskedBy(1L)
        val b = questionAskedBy(2L)
        val c = questionAskedBy(3L)
        val d = questionAskedBy(4L)
        useCase.execute(a, b)
        useCase.execute(c, d)

        assertFailsWith<ClustersAlreadyDistinctException> { useCase.execute(a, c) }
    }

    @Test
    fun `rejects marking a question as the same problem as itself`() {
        val a = questionAskedBy(1L)

        assertFailsWith<CannotClusterWithSelfException> { useCase.execute(a, a) }
    }

    @Test
    fun `rejects a question id that does not exist`() {
        val a = questionAskedBy(1L)

        assertFailsWith<QuestionNotFoundException> { useCase.execute(a, 999L) }
    }
}
