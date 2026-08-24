package com.quno.qunobackend.application.question.usecase

import com.quno.qunobackend.application.question.dto.CreateQuestionCommand
import com.quno.qunobackend.domain.question.QuestionStatus
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class CreateQuestionUseCaseTest {
    private val questionRepository = InMemoryQuestionRepository()
    private val questionVersionRepository = InMemoryQuestionVersionRepository()
    private val useCase = CreateQuestionUseCase(questionRepository, questionVersionRepository)

    @Test
    fun `creates a question with an OPEN status and a first version`() {
        val result = useCase.execute(
            CreateQuestionCommand(
                authorId = 1L,
                title = "Redis timeout",
                body = "Connections drop intermittently",
                environment = "Spring Boot 4 / Redis 8",
                logs = null,
            ),
        )

        assertEquals("Redis timeout", result.title)
        assertEquals(QuestionStatus.OPEN, result.status)
        assertEquals(1, result.versionNumber)
    }

    @Test
    fun `wires the question's latest version pointer to the saved version`() {
        val result = useCase.execute(
            CreateQuestionCommand(authorId = 1L, title = "Redis timeout", body = "body", environment = null, logs = null),
        )

        val question = questionRepository.findById(result.id)
        assertNotNull(question)
        val latestVersion = questionVersionRepository.findById(requireNotNull(question.latestVersionId))
        assertNotNull(latestVersion)
        assertEquals("body", latestVersion.bodyMarkdown)
    }
}
