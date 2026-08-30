package com.quno.qunobackend.application.vote.usecase

import com.quno.qunobackend.application.answer.usecase.InMemoryAnswerRepository
import com.quno.qunobackend.application.question.dto.CreateQuestionCommand
import com.quno.qunobackend.application.question.usecase.CreateQuestionUseCase
import com.quno.qunobackend.application.question.usecase.InMemoryQuestionRepository
import com.quno.qunobackend.application.question.usecase.InMemoryQuestionVersionRepository
import com.quno.qunobackend.application.tag.usecase.InMemoryQuestionTagRepository
import com.quno.qunobackend.application.tag.usecase.InMemoryTagRepository
import com.quno.qunobackend.application.vote.dto.CastVoteCommand
import com.quno.qunobackend.domain.vote.VoteTargetType
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class RetractVoteUseCaseTest {
    private val questionRepository = InMemoryQuestionRepository()
    private val answerRepository = InMemoryAnswerRepository()
    private val tagRepository = InMemoryTagRepository()
    private val createQuestionUseCase = CreateQuestionUseCase(
        questionRepository, InMemoryQuestionVersionRepository(), tagRepository, InMemoryQuestionTagRepository(tagRepository),
    )
    private val voteRepository = InMemoryVoteRepository()
    private val castVoteUseCase = CastVoteUseCase(questionRepository, answerRepository, voteRepository)
    private val retractVoteUseCase = RetractVoteUseCase(voteRepository)

    private fun aQuestion(): Long = createQuestionUseCase.execute(
        CreateQuestionCommand(authorId = 1L, title = "t", body = "body", environment = null, logs = null),
    ).id

    @Test
    fun `retracting a vote removes it from the score`() {
        val questionId = aQuestion()
        castVoteUseCase.execute(CastVoteCommand(voterId = 10L, targetType = VoteTargetType.QUESTION, targetId = questionId, value = 1))

        retractVoteUseCase.execute(voterId = 10L, targetType = VoteTargetType.QUESTION, targetId = questionId)

        assertEquals(0L, voteRepository.sumScore(VoteTargetType.QUESTION, questionId))
    }

    @Test
    fun `retracting a vote that was never cast stays a no-op`() {
        val questionId = aQuestion()

        retractVoteUseCase.execute(voterId = 999L, targetType = VoteTargetType.QUESTION, targetId = questionId)

        assertEquals(0L, voteRepository.sumScore(VoteTargetType.QUESTION, questionId))
    }
}
