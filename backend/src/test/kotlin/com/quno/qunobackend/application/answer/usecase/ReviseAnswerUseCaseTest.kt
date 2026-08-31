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
import com.quno.qunobackend.domain.answer.AnswerAccessDeniedException
import com.quno.qunobackend.domain.common.OutboxEventTypes
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class ReviseAnswerUseCaseTest {
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

    private fun aQuestion(authorId: Long = 1L): Long = createQuestionUseCase.execute(
        CreateQuestionCommand(authorId = authorId, title = "t", body = "body", environment = null, logs = null),
    ).id

    private fun anAnswer(questionId: Long, authorId: Long = 2L): Long =
        writeAnswerUseCase.execute(WriteAnswerCommand(questionId, authorId, "v1 body")).id

    @Test
    fun `appends a new version and updates the cached body`() {
        val questionId = aQuestion()
        val answerId = anAnswer(questionId)

        val result = reviseAnswerUseCase.execute(ReviseAnswerCommand(answerId, actorId = 2L, body = "v2 body with more detail"))

        assertEquals(2, result.versionNumber)
        assertEquals(questionId, result.questionId)
        assertEquals("v2 body with more detail", answerRepository.findById(answerId)?.bodyMarkdown)

        val v2 = answerVersionRepository.findByAnswerIdAndVersionNumber(answerId, 2)
        assertEquals("v2 body with more detail", v2?.bodyMarkdown)
    }

    @Test
    fun `keeps incrementing the version number across multiple revisions`() {
        val questionId = aQuestion()
        val answerId = anAnswer(questionId)

        reviseAnswerUseCase.execute(ReviseAnswerCommand(answerId, 2L, "v2"))
        val third = reviseAnswerUseCase.execute(ReviseAnswerCommand(answerId, 2L, "v3"))

        assertEquals(3, third.versionNumber)
        assertEquals(3, answerVersionRepository.findAllByAnswerIdOrderByVersionNumberAsc(answerId).size)
    }

    @Test
    fun `rejects a revision from someone other than the answer's author`() {
        val questionId = aQuestion()
        val answerId = anAnswer(questionId, authorId = 2L)

        assertFailsWith<AnswerAccessDeniedException> {
            reviseAnswerUseCase.execute(ReviseAnswerCommand(answerId, actorId = 3L, body = "not mine to edit"))
        }
    }

    @Test
    fun `records an ANSWER_REVISION outbox event with the question author`() {
        val authorId = 1L
        val questionId = aQuestion(authorId = authorId)
        val answerId = anAnswer(questionId)

        reviseAnswerUseCase.execute(ReviseAnswerCommand(answerId, 2L, "v2"))

        val event = outboxEventRepository.events.single { it.eventType == OutboxEventTypes.ANSWER_REVISION }
        assertEquals(questionId, event.aggregateId)
        assertTrue(event.payload.contains(""""questionAuthorId":$authorId"""))
    }
}
