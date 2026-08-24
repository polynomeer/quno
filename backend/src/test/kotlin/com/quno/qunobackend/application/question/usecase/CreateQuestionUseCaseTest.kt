package com.quno.qunobackend.application.question.usecase

import com.quno.qunobackend.application.question.dto.CreateQuestionCommand
import com.quno.qunobackend.domain.question.Question
import com.quno.qunobackend.domain.question.QuestionRepository
import com.quno.qunobackend.domain.question.QuestionStatus
import com.quno.qunobackend.domain.question.QuestionVersion
import com.quno.qunobackend.domain.question.QuestionVersionRepository
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

private class InMemoryQuestionRepository : QuestionRepository {
    private val byId = mutableMapOf<Long, Question>()
    private var nextId = 1L

    override fun save(question: Question): Question {
        val saved = if (question.id == null) {
            Question.reconstitute(
                id = nextId++,
                authorId = question.authorId,
                title = question.title,
                status = question.status,
                latestVersionId = question.latestVersionId,
                acceptedAnswerId = question.acceptedAnswerId,
                deletedAt = question.deletedAt,
                createdAt = question.createdAt,
                updatedAt = question.updatedAt,
            )
        } else {
            question
        }
        byId[requireNotNull(saved.id)] = saved
        return saved
    }

    override fun findById(id: Long): Question? = byId[id]
}

private class InMemoryQuestionVersionRepository : QuestionVersionRepository {
    private val byId = mutableMapOf<Long, QuestionVersion>()
    private var nextId = 1L

    override fun save(version: QuestionVersion): QuestionVersion {
        val saved = if (version.id == null) {
            QuestionVersion.reconstitute(
                id = nextId++,
                questionId = version.questionId,
                versionNumber = version.versionNumber,
                title = version.title,
                bodyMarkdown = version.bodyMarkdown,
                environment = version.environment,
                logs = version.logs,
                createdBy = version.createdBy,
                createdAt = version.createdAt,
            )
        } else {
            version
        }
        byId[requireNotNull(saved.id)] = saved
        return saved
    }

    override fun findById(id: Long): QuestionVersion? = byId[id]
}

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
