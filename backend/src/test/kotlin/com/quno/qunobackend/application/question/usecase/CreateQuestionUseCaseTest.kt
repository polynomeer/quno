package com.quno.qunobackend.application.question.usecase

import com.quno.qunobackend.application.question.dto.CreateQuestionCommand
import com.quno.qunobackend.application.tag.usecase.InMemoryQuestionTagRepository
import com.quno.qunobackend.application.tag.usecase.InMemoryTagRepository
import com.quno.qunobackend.domain.question.QuestionStatus
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class CreateQuestionUseCaseTest {
    private val questionRepository = InMemoryQuestionRepository()
    private val questionVersionRepository = InMemoryQuestionVersionRepository()
    private val tagRepository = InMemoryTagRepository()
    private val questionTagRepository = InMemoryQuestionTagRepository(tagRepository)
    private val useCase = CreateQuestionUseCase(questionRepository, questionVersionRepository, tagRepository, questionTagRepository)

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

    @Test
    fun `attaches find-or-create tags to the question`() {
        val result = useCase.execute(
            CreateQuestionCommand(
                authorId = 1L,
                title = "Redis timeout",
                body = "body",
                environment = null,
                logs = null,
                tagNames = listOf("redis", "spring-boot", "redis"),
            ),
        )

        val tagNames = questionTagRepository.findTagsByQuestionId(result.id).map { it.name }
        assertEquals(listOf("redis", "spring-boot"), tagNames.sorted())
    }

    @Test
    fun `tag names that only differ by case reuse the same tag (same slug)`() {
        val result = useCase.execute(
            CreateQuestionCommand(
                authorId = 1L,
                title = "t",
                body = "body",
                environment = null,
                logs = null,
                tagNames = listOf("Kotlin", "Coroutines", "KOTLIN"),
            ),
        )

        val tags = questionTagRepository.findTagsByQuestionId(result.id)
        assertEquals(2, tags.size)
        assertEquals(setOf("kotlin", "coroutines"), tags.map { it.slug }.toSet())
    }
}
