package com.quno.qunobackend.application.question.usecase

import com.quno.qunobackend.application.question.dto.CreateQuestionCommand
import com.quno.qunobackend.application.question.dto.ForkQuestionCommand
import com.quno.qunobackend.application.tag.usecase.InMemoryQuestionTagRepository
import com.quno.qunobackend.application.tag.usecase.InMemoryTagRepository
import com.quno.qunobackend.domain.question.QuestionNotFoundException
import com.quno.qunobackend.domain.question.QuestionStatus
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

class ForkQuestionUseCaseTest {
    private val questionRepository = InMemoryQuestionRepository()
    private val questionVersionRepository = InMemoryQuestionVersionRepository()
    private val tagRepository = InMemoryTagRepository()
    private val questionTagRepository = InMemoryQuestionTagRepository(tagRepository)
    private val createQuestionUseCase = CreateQuestionUseCase(questionRepository, questionVersionRepository, tagRepository, questionTagRepository)
    private val forkQuestionUseCase = ForkQuestionUseCase(questionRepository, questionVersionRepository, questionTagRepository)

    private fun anOrigin(): Long = createQuestionUseCase.execute(
        CreateQuestionCommand(
            authorId = 1L,
            title = "RedisCommandTimeoutException",
            body = "Fails on Redis 7 under load",
            environment = "Spring Boot 4 / Redis 7",
            logs = "RedisCommandTimeoutException: ...",
            tagNames = listOf("redis", "spring-boot"),
        ),
    ).id

    @Test
    fun `forking copies the origin's current content and tags to a new question owned by the forker`() {
        val originId = anOrigin()

        val result = forkQuestionUseCase.execute(ForkQuestionCommand(originQuestionId = originId, actorId = 2L))

        assertEquals("RedisCommandTimeoutException", result.title)
        assertEquals(QuestionStatus.OPEN, result.status)
        assertEquals(1, result.versionNumber)

        val forked = requireNotNull(questionRepository.findById(result.id))
        assertEquals(2L, forked.authorId)
        assertEquals(originId, forked.originQuestionId)

        val version = requireNotNull(questionVersionRepository.findByQuestionIdAndVersionNumber(result.id, 1))
        assertEquals("Fails on Redis 7 under load", version.bodyMarkdown)
        assertEquals("Spring Boot 4 / Redis 7", version.environment)

        assertEquals(setOf("redis", "spring-boot"), questionTagRepository.findTagsByQuestionId(result.id).map { it.name }.toSet())
    }

    @Test
    fun `a forked question does not join the origin's cluster`() {
        val originId = anOrigin()

        val result = forkQuestionUseCase.execute(ForkQuestionCommand(originQuestionId = originId, actorId = 2L))

        assertNull(questionRepository.findById(result.id)!!.clusterId)
    }

    @Test
    fun `forking your own question is allowed`() {
        val originId = anOrigin()

        val result = forkQuestionUseCase.execute(ForkQuestionCommand(originQuestionId = originId, actorId = 1L))

        assertEquals(1L, questionRepository.findById(result.id)!!.authorId)
    }

    @Test
    fun `rejects forking a question that does not exist`() {
        assertFailsWith<QuestionNotFoundException> {
            forkQuestionUseCase.execute(ForkQuestionCommand(originQuestionId = 999L, actorId = 2L))
        }
    }
}
