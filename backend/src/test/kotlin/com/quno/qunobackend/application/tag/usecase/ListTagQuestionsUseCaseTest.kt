package com.quno.qunobackend.application.tag.usecase

import com.quno.qunobackend.application.common.QuestionSummaryHydrator
import com.quno.qunobackend.application.question.dto.CreateQuestionCommand
import com.quno.qunobackend.application.question.usecase.CreateQuestionUseCase
import com.quno.qunobackend.application.question.usecase.InMemoryQuestionRepository
import com.quno.qunobackend.application.question.usecase.InMemoryQuestionVersionRepository
import com.quno.qunobackend.application.vote.usecase.InMemoryVoteRepository
import com.quno.qunobackend.domain.tag.Tag
import com.quno.qunobackend.domain.tag.TagNotFoundException
import com.quno.qunobackend.domain.tag.TagQuestionSort
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class ListTagQuestionsUseCaseTest {
    private val questionRepository = InMemoryQuestionRepository()
    private val tagRepository = InMemoryTagRepository()
    private val questionTagRepository = InMemoryQuestionTagRepository(tagRepository)
    private val createQuestionUseCase = CreateQuestionUseCase(
        questionRepository, InMemoryQuestionVersionRepository(), tagRepository, questionTagRepository,
    )
    private val tagStatsRepository = InMemoryTagStatsRepository()
    private val hydrator = QuestionSummaryHydrator(questionRepository, questionTagRepository, InMemoryVoteRepository())
    private val useCase = ListTagQuestionsUseCase(tagRepository, tagStatsRepository, hydrator)

    private fun question(title: String): Long =
        createQuestionUseCase.execute(
            CreateQuestionCommand(authorId = 1L, title = title, body = "body", environment = null, logs = null),
        ).id

    @Test
    fun `hydrates ids the repository ranks for the requested sort`() {
        val tagId = requireNotNull(tagRepository.save(Tag.create("Kotlin")).id)
        val q1 = question("Coroutine leak")
        val q2 = question("Null safety question")
        tagStatsRepository.questionIdsBySort = mapOf(TagQuestionSort.TOP to listOf(q2, q1))

        val result = useCase.execute(tagId, TagQuestionSort.TOP)

        assertEquals(listOf(q2, q1), result.map { it.id })
        assertEquals(TagQuestionSort.TOP, tagStatsRepository.lastSort)
    }

    @Test
    fun `defaults are not assumed — passes the given sort through untouched`() {
        val tagId = requireNotNull(tagRepository.save(Tag.create("Kotlin")).id)

        useCase.execute(tagId, TagQuestionSort.UNANSWERED)

        assertEquals(TagQuestionSort.UNANSWERED, tagStatsRepository.lastSort)
    }

    @Test
    fun `silently skips ids that no longer resolve to a question`() {
        val tagId = requireNotNull(tagRepository.save(Tag.create("Kotlin")).id)
        tagStatsRepository.questionIdsBySort = mapOf(TagQuestionSort.LATEST to listOf(999L))

        assertTrue(useCase.execute(tagId, TagQuestionSort.LATEST).isEmpty())
    }

    @Test
    fun `rejects a tag that does not exist`() {
        assertFailsWith<TagNotFoundException> { useCase.execute(999L, TagQuestionSort.LATEST) }
    }
}
