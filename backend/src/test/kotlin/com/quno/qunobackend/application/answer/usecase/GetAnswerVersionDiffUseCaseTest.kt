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
import com.quno.qunobackend.domain.question.DiffLineType
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class GetAnswerVersionDiffUseCaseTest {
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
    private val getAnswerVersionDiffUseCase = GetAnswerVersionDiffUseCase(answerRepository, answerVersionRepository)

    @Test
    fun `diffs two versions line by line`() {
        val questionId = createQuestionUseCase.execute(
            CreateQuestionCommand(authorId = 1L, title = "t", body = "body", environment = null, logs = null),
        ).id
        val answerId = writeAnswerUseCase.execute(WriteAnswerCommand(questionId, 2L, "line one\nline two")).id
        reviseAnswerUseCase.execute(ReviseAnswerCommand(answerId, 2L, "line one\nline three"))

        val diff = getAnswerVersionDiffUseCase.execute(answerId, fromVersion = 1, toVersion = 2)

        assertEquals(1, diff.fromVersion)
        assertEquals(2, diff.toVersion)
        assertEquals(DiffLineType.EQUAL, diff.lines[0].type)
        assertEquals(DiffLineType.REMOVED, diff.lines[1].type)
        assertEquals(DiffLineType.ADDED, diff.lines[2].type)
    }
}
