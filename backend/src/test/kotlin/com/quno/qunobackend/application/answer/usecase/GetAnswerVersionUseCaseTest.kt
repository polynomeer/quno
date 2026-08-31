package com.quno.qunobackend.application.answer.usecase

import com.quno.qunobackend.application.answer.dto.ReviseAnswerCommand
import com.quno.qunobackend.application.answer.dto.WriteAnswerCommand
import com.quno.qunobackend.application.common.AnswerResultAssembler
import com.quno.qunobackend.application.common.InMemoryOutboxEventRepository
import com.quno.qunobackend.application.question.dto.CreateQuestionCommand
import com.quno.qunobackend.application.question.usecase.CreateQuestionUseCase
import com.quno.qunobackend.application.question.usecase.InMemoryQuestionRepository
import com.quno.qunobackend.application.question.usecase.InMemoryQuestionVersionRepository
import com.quno.qunobackend.application.tag.usecase.InMemoryQuestionTagRepository
import com.quno.qunobackend.application.tag.usecase.InMemoryTagRepository
import com.quno.qunobackend.application.vote.usecase.InMemoryVoteRepository
import com.quno.qunobackend.domain.answer.AnswerNotFoundException
import com.quno.qunobackend.domain.answer.AnswerVersionNotFoundException
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class GetAnswerVersionUseCaseTest {
    private val questionRepository = InMemoryQuestionRepository()
    private val questionVersionRepository = InMemoryQuestionVersionRepository()
    private val tagRepository = InMemoryTagRepository()
    private val answerRepository = InMemoryAnswerRepository()
    private val answerVersionRepository = InMemoryAnswerVersionRepository()
    private val outboxEventRepository = InMemoryOutboxEventRepository()
    private val createQuestionUseCase = CreateQuestionUseCase(
        questionRepository, questionVersionRepository, tagRepository, InMemoryQuestionTagRepository(tagRepository),
    )
    private val writeAnswerUseCase = WriteAnswerUseCase(
        questionRepository, questionVersionRepository, answerRepository, answerVersionRepository, outboxEventRepository,
        AnswerResultAssembler(questionRepository, questionVersionRepository, InMemoryVoteRepository()),
    )
    private val reviseAnswerUseCase = ReviseAnswerUseCase(answerRepository, answerVersionRepository, questionRepository, outboxEventRepository)
    private val getAnswerVersionUseCase = GetAnswerVersionUseCase(answerRepository, answerVersionRepository)

    private fun aQuestion(): Long = createQuestionUseCase.execute(
        CreateQuestionCommand(authorId = 1L, title = "t", body = "body", environment = null, logs = null),
    ).id

    @Test
    fun `returns a specific version's content`() {
        val answerId = writeAnswerUseCase.execute(WriteAnswerCommand(aQuestion(), 2L, "v1 body")).id
        reviseAnswerUseCase.execute(ReviseAnswerCommand(answerId, 2L, "v2 body"))

        val v1 = getAnswerVersionUseCase.execute(answerId, 1)

        assertEquals("v1 body", v1.body)
        assertEquals(2L, v1.createdBy)
    }

    @Test
    fun `rejects an answer that does not exist`() {
        assertFailsWith<AnswerNotFoundException> { getAnswerVersionUseCase.execute(999L, 1) }
    }

    @Test
    fun `rejects a version number that does not exist`() {
        val answerId = writeAnswerUseCase.execute(WriteAnswerCommand(aQuestion(), 2L, "v1 body")).id

        assertFailsWith<AnswerVersionNotFoundException> { getAnswerVersionUseCase.execute(answerId, 2) }
    }
}
