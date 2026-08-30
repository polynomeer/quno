package com.quno.qunobackend.application.save.usecase

import com.quno.qunobackend.application.question.dto.CreateQuestionCommand
import com.quno.qunobackend.application.question.usecase.CreateQuestionUseCase
import com.quno.qunobackend.application.question.usecase.InMemoryQuestionRepository
import com.quno.qunobackend.application.question.usecase.InMemoryQuestionVersionRepository
import com.quno.qunobackend.application.tag.usecase.InMemoryQuestionTagRepository
import com.quno.qunobackend.application.tag.usecase.InMemoryTagRepository
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class ListMySavesUseCaseTest {
    private val questionRepository = InMemoryQuestionRepository()
    private val tagRepository = InMemoryTagRepository()
    private val createQuestionUseCase = CreateQuestionUseCase(
        questionRepository, InMemoryQuestionVersionRepository(), tagRepository, InMemoryQuestionTagRepository(tagRepository),
    )
    private val saveRepository = InMemorySaveRepository()
    private val saveUseCase = SaveQuestionUseCase(questionRepository, saveRepository)
    private val listUseCase = ListMySavesUseCase(saveRepository, questionRepository)

    @Test
    fun `lists the questions a user saved`() {
        val questionId = createQuestionUseCase.execute(
            CreateQuestionCommand(authorId = 1L, title = "Redis timeout", body = "body", environment = null, logs = null),
        ).id
        saveUseCase.execute(userId = 10L, questionId = questionId)

        val result = listUseCase.execute(userId = 10L)

        assertEquals(listOf("Redis timeout"), result.map { it.title })
    }

    @Test
    fun `returns nothing for a user with no saves`() {
        assertEquals(emptyList(), listUseCase.execute(userId = 999L))
    }
}
