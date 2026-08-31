package com.quno.qunobackend.application.question.usecase

import com.quno.qunobackend.application.common.QuestionSummaryHydrator
import com.quno.qunobackend.application.question.dto.CreateQuestionCommand
import com.quno.qunobackend.application.question.dto.ForkQuestionCommand
import com.quno.qunobackend.application.tag.usecase.InMemoryQuestionTagRepository
import com.quno.qunobackend.application.tag.usecase.InMemoryTagRepository
import com.quno.qunobackend.application.vote.usecase.InMemoryVoteRepository
import com.quno.qunobackend.domain.question.QuestionNotFoundException
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class ListQuestionForksUseCaseTest {
    private val questionRepository = InMemoryQuestionRepository()
    private val questionVersionRepository = InMemoryQuestionVersionRepository()
    private val tagRepository = InMemoryTagRepository()
    private val questionTagRepository = InMemoryQuestionTagRepository(tagRepository)
    private val createQuestionUseCase = CreateQuestionUseCase(questionRepository, questionVersionRepository, tagRepository, questionTagRepository)
    private val forkQuestionUseCase = ForkQuestionUseCase(questionRepository, questionVersionRepository, questionTagRepository)
    private val hydrator = QuestionSummaryHydrator(questionRepository, questionTagRepository, InMemoryVoteRepository())
    private val listQuestionForksUseCase = ListQuestionForksUseCase(questionRepository, hydrator)

    private fun aQuestion(title: String = "t"): Long = createQuestionUseCase.execute(
        CreateQuestionCommand(authorId = 1L, title = title, body = "body", environment = null, logs = null),
    ).id

    @Test
    fun `lists every question forked from the origin`() {
        val originId = aQuestion()
        val fork1 = forkQuestionUseCase.execute(ForkQuestionCommand(originId, actorId = 2L)).id
        val fork2 = forkQuestionUseCase.execute(ForkQuestionCommand(originId, actorId = 3L)).id

        val result = listQuestionForksUseCase.execute(originId)

        assertEquals(setOf(fork1, fork2), result.map { it.id }.toSet())
    }

    @Test
    fun `returns nothing for a question with no forks`() {
        val originId = aQuestion()

        assertEquals(emptyList(), listQuestionForksUseCase.execute(originId))
    }

    @Test
    fun `rejects a question that does not exist`() {
        assertFailsWith<QuestionNotFoundException> { listQuestionForksUseCase.execute(999L) }
    }
}
