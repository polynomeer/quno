package com.quno.qunobackend.application.save.usecase

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

class SaveQuestionUseCaseTest {
    private val questionRepository = InMemoryQuestionRepository()
    private val tagRepository = InMemoryTagRepository()
    private val createQuestionUseCase = CreateQuestionUseCase(
        questionRepository, InMemoryQuestionVersionRepository(), tagRepository, InMemoryQuestionTagRepository(tagRepository),
    )
    private val saveRepository = InMemorySaveRepository()
    private val saveUseCase = SaveQuestionUseCase(questionRepository, saveRepository)
    private val unsaveUseCase = UnsaveQuestionUseCase(saveRepository)

    private fun aQuestion(): Long = createQuestionUseCase.execute(
        CreateQuestionCommand(authorId = 1L, title = "t", body = "body", environment = null, logs = null),
    ).id

    @Test
    fun `saving an existing question registers the save`() {
        val questionId = aQuestion()

        saveUseCase.execute(userId = 10L, questionId = questionId)

        assertTrue(saveRepository.isSaved(10L, questionId))
    }

    @Test
    fun `saving twice stays idempotent`() {
        val questionId = aQuestion()

        saveUseCase.execute(userId = 10L, questionId = questionId)
        saveUseCase.execute(userId = 10L, questionId = questionId)

        assertTrue(saveRepository.findSavedQuestionIds(10L).size == 1)
    }

    @Test
    fun `saving your own question is allowed`() {
        val questionId = aQuestion()

        saveUseCase.execute(userId = 1L, questionId = questionId)

        assertTrue(saveRepository.isSaved(1L, questionId))
    }

    @Test
    fun `rejects saving a question that does not exist`() {
        assertFailsWith<QuestionNotFoundException> { saveUseCase.execute(userId = 10L, questionId = 999L) }
    }

    @Test
    fun `unsave clears the save`() {
        val questionId = aQuestion()
        saveUseCase.execute(userId = 10L, questionId = questionId)

        unsaveUseCase.execute(userId = 10L, questionId = questionId)

        assertFalse(saveRepository.isSaved(10L, questionId))
    }
}
