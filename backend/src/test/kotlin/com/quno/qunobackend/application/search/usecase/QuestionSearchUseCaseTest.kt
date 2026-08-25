package com.quno.qunobackend.application.search.usecase

import com.quno.qunobackend.application.common.QuestionSummaryHydrator
import com.quno.qunobackend.application.question.dto.CreateQuestionCommand
import com.quno.qunobackend.application.question.usecase.CreateQuestionUseCase
import com.quno.qunobackend.application.question.usecase.InMemoryQuestionRepository
import com.quno.qunobackend.application.question.usecase.InMemoryQuestionVersionRepository
import com.quno.qunobackend.application.tag.usecase.InMemoryQuestionTagRepository
import com.quno.qunobackend.application.tag.usecase.InMemoryTagRepository
import com.quno.qunobackend.domain.question.QuestionNotFoundException
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class QuestionSearchUseCaseTest {
    private val questionRepository = InMemoryQuestionRepository()
    private val tagRepository = InMemoryTagRepository()
    private val questionTagRepository = InMemoryQuestionTagRepository(tagRepository)
    private val createUseCase = CreateQuestionUseCase(
        questionRepository, InMemoryQuestionVersionRepository(), tagRepository, questionTagRepository,
    )
    private val searchRepository = InMemorySearchRepository()
    private val hydrator = QuestionSummaryHydrator(questionRepository, questionTagRepository)
    private val useCase = QuestionSearchUseCase(searchRepository, questionRepository, hydrator)

    private fun question(title: String, tags: List<String> = emptyList()): Long =
        createUseCase.execute(
            CreateQuestionCommand(authorId = 1L, title = title, body = "body", environment = null, logs = null, tagNames = tags),
        ).id

    @Test
    fun `search hydrates ids in the order the repository returns them`() {
        val redis = question("Redis timeout", listOf("redis"))
        val kafka = question("Kafka lag", listOf("kafka"))
        searchRepository.searchResults = listOf(kafka, redis)

        val result = useCase.search("anything")

        assertEquals(listOf(kafka, redis), result.map { it.id })
        assertEquals(listOf("redis"), result.last().tags)
    }

    @Test
    fun `search silently skips ids that no longer resolve to a question`() {
        searchRepository.searchResults = listOf(999L)

        assertTrue(useCase.search("anything").isEmpty())
    }

    @Test
    fun `related requires the question to exist`() {
        assertFailsWith<QuestionNotFoundException> { useCase.related(999L) }
    }

    @Test
    fun `related returns the repository's ranked ids`() {
        val q1 = question("Redis timeout")
        val q2 = question("Redis pool exhaustion")
        searchRepository.relatedResults = mapOf(q1 to listOf(q2))

        val result = useCase.related(q1)

        assertEquals(listOf(q2), result.map { it.id })
    }
}
