package com.quno.qunobackend.application.question.usecase

import com.quno.qunobackend.application.question.dto.CreateQuestionCommand
import com.quno.qunobackend.application.question.dto.ReviseQuestionCommand
import com.quno.qunobackend.application.tag.usecase.InMemoryQuestionTagRepository
import com.quno.qunobackend.application.tag.usecase.InMemoryTagRepository
import com.quno.qunobackend.domain.question.DiffLineType
import com.quno.qunobackend.domain.question.QuestionVersionNotFoundException
import org.junit.jupiter.api.Test
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class GetQuestionVersionDiffUseCaseTest {
    private val questionRepository = InMemoryQuestionRepository()
    private val questionVersionRepository = InMemoryQuestionVersionRepository()
    private val tagRepository = InMemoryTagRepository()
    private val createUseCase = CreateQuestionUseCase(
        questionRepository, questionVersionRepository, tagRepository, InMemoryQuestionTagRepository(tagRepository),
    )
    private val reviseUseCase = ReviseQuestionUseCase(questionRepository, questionVersionRepository)
    private val diffUseCase = GetQuestionVersionDiffUseCase(questionRepository, questionVersionRepository)

    @Test
    fun `diffs the bodies of two versions`() {
        val questionId = createUseCase.execute(
            CreateQuestionCommand(authorId = 1L, title = "t", body = "Spring Boot 3.x\nsame line", environment = null, logs = null),
        ).id
        reviseUseCase.execute(ReviseQuestionCommand(questionId, 1L, "t", "Spring Boot 4.x\nsame line", null, null))

        val diff = diffUseCase.execute(questionId, fromVersion = 1, toVersion = 2)

        assertTrue(diff.lines.any { it.type == DiffLineType.REMOVED && it.text == "Spring Boot 3.x" })
        assertTrue(diff.lines.any { it.type == DiffLineType.ADDED && it.text == "Spring Boot 4.x" })
        assertTrue(diff.lines.any { it.type == DiffLineType.EQUAL && it.text == "same line" })
    }

    @Test
    fun `throws when the requested version does not exist`() {
        val questionId = createUseCase.execute(
            CreateQuestionCommand(authorId = 1L, title = "t", body = "body", environment = null, logs = null),
        ).id

        assertFailsWith<QuestionVersionNotFoundException> {
            diffUseCase.execute(questionId, fromVersion = 1, toVersion = 2)
        }
    }
}
