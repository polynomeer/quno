package com.quno.qunobackend.application.watch.usecase

import com.quno.qunobackend.application.question.dto.CreateQuestionCommand
import com.quno.qunobackend.application.question.usecase.CreateQuestionUseCase
import com.quno.qunobackend.application.question.usecase.InMemoryQuestionRepository
import com.quno.qunobackend.application.question.usecase.InMemoryQuestionVersionRepository
import com.quno.qunobackend.application.tag.usecase.InMemoryQuestionTagRepository
import com.quno.qunobackend.application.tag.usecase.InMemoryTagRepository
import com.quno.qunobackend.domain.question.QuestionNotFoundException
import org.junit.jupiter.api.Test
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class WatchQuestionUseCaseTest {
    private val questionRepository = InMemoryQuestionRepository()
    private val tagRepository = InMemoryTagRepository()
    private val createQuestionUseCase = CreateQuestionUseCase(
        questionRepository, InMemoryQuestionVersionRepository(), tagRepository, InMemoryQuestionTagRepository(tagRepository),
    )
    private val watchRepository = InMemoryWatchRepository()
    private val watchUseCase = WatchQuestionUseCase(questionRepository, watchRepository)
    private val unwatchUseCase = UnwatchQuestionUseCase(watchRepository)

    private fun aQuestion(): Long = createQuestionUseCase.execute(
        CreateQuestionCommand(authorId = 1L, title = "t", body = "body", environment = null, logs = null),
    ).id

    @Test
    fun `watching an existing question registers the watch`() {
        val questionId = aQuestion()

        watchUseCase.execute(userId = 10L, questionId = questionId)

        assertTrue(watchRepository.isWatching(10L, questionId))
    }

    @Test
    fun `watching twice stays idempotent`() {
        val questionId = aQuestion()

        watchUseCase.execute(userId = 10L, questionId = questionId)
        watchUseCase.execute(userId = 10L, questionId = questionId)

        assertTrue(watchRepository.findWatchedQuestionIds(10L).size == 1)
    }

    @Test
    fun `rejects watching a question that does not exist`() {
        assertFailsWith<QuestionNotFoundException> { watchUseCase.execute(userId = 10L, questionId = 999L) }
    }

    @Test
    fun `unwatch clears the watch`() {
        val questionId = aQuestion()
        watchUseCase.execute(userId = 10L, questionId = questionId)

        unwatchUseCase.execute(userId = 10L, questionId = questionId)

        assertFalse(watchRepository.isWatching(10L, questionId))
    }
}
