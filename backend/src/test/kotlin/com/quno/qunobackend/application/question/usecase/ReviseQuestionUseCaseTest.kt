package com.quno.qunobackend.application.question.usecase

import com.quno.qunobackend.application.question.dto.CreateQuestionCommand
import com.quno.qunobackend.application.question.dto.ReviseQuestionCommand
import com.quno.qunobackend.domain.question.QuestionAccessDeniedException
import com.quno.qunobackend.domain.question.QuestionStatus
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class ReviseQuestionUseCaseTest {
    private val questionRepository = InMemoryQuestionRepository()
    private val questionVersionRepository = InMemoryQuestionVersionRepository()
    private val createUseCase = CreateQuestionUseCase(questionRepository, questionVersionRepository)
    private val reviseUseCase = ReviseQuestionUseCase(questionRepository, questionVersionRepository)

    private fun createQuestion(authorId: Long = 1L): Long =
        createUseCase.execute(
            CreateQuestionCommand(authorId = authorId, title = "Redis timeout", body = "v1 body", environment = null, logs = null),
        ).id

    @Test
    fun `appends a new version and moves the status to UPDATED`() {
        val questionId = createQuestion()

        val result = reviseUseCase.execute(
            ReviseQuestionCommand(
                questionId = questionId,
                actorId = 1L,
                title = "Redis timeout (updated)",
                body = "v2 body with full stacktrace",
                environment = "Spring Boot 4 / Redis 8",
                logs = "RedisCommandTimeoutException ...",
            ),
        )

        assertEquals(2, result.versionNumber)
        assertEquals(QuestionStatus.UPDATED, result.status)

        val v2 = questionVersionRepository.findByQuestionIdAndVersionNumber(questionId, 2)
        assertEquals("v2 body with full stacktrace", v2?.bodyMarkdown)
    }

    @Test
    fun `keeps incrementing the version number across multiple revisions`() {
        val questionId = createQuestion()

        reviseUseCase.execute(ReviseQuestionCommand(questionId, 1L, "t2", "b2", null, null))
        val third = reviseUseCase.execute(ReviseQuestionCommand(questionId, 1L, "t3", "b3", null, null))

        assertEquals(3, third.versionNumber)
        assertEquals(3, questionVersionRepository.findAllByQuestionIdOrderByVersionNumberAsc(questionId).size)
    }

    @Test
    fun `rejects a revision from someone other than the question author`() {
        val questionId = createQuestion(authorId = 1L)

        assertFailsWith<QuestionAccessDeniedException> {
            reviseUseCase.execute(ReviseQuestionCommand(questionId, actorId = 2L, title = "t2", body = "b2", environment = null, logs = null))
        }
    }
}
