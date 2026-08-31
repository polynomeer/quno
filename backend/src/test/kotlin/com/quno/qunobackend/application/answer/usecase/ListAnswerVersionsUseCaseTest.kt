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
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class ListAnswerVersionsUseCaseTest {
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
    private val listAnswerVersionsUseCase = ListAnswerVersionsUseCase(answerRepository, answerVersionRepository)

    @Test
    fun `lists versions oldest first`() {
        val questionId = createQuestionUseCase.execute(
            CreateQuestionCommand(authorId = 1L, title = "t", body = "body", environment = null, logs = null),
        ).id
        val answerId = writeAnswerUseCase.execute(WriteAnswerCommand(questionId, 2L, "v1")).id
        reviseAnswerUseCase.execute(ReviseAnswerCommand(answerId, 2L, "v2"))

        val result = listAnswerVersionsUseCase.execute(answerId)

        assertEquals(listOf(1, 2), result.map { it.versionNumber })
    }

    @Test
    fun `rejects listing versions for an answer that does not exist`() {
        assertFailsWith<AnswerNotFoundException> { listAnswerVersionsUseCase.execute(999L) }
    }
}
