package com.quno.qunobackend.application.vote.usecase

import com.quno.qunobackend.application.answer.usecase.InMemoryAnswerRepository
import com.quno.qunobackend.application.question.dto.CreateQuestionCommand
import com.quno.qunobackend.application.question.usecase.CreateQuestionUseCase
import com.quno.qunobackend.application.question.usecase.InMemoryQuestionRepository
import com.quno.qunobackend.application.question.usecase.InMemoryQuestionVersionRepository
import com.quno.qunobackend.application.tag.usecase.InMemoryQuestionTagRepository
import com.quno.qunobackend.application.tag.usecase.InMemoryTagRepository
import com.quno.qunobackend.application.vote.dto.CastVoteCommand
import com.quno.qunobackend.domain.answer.Answer
import com.quno.qunobackend.domain.question.QuestionNotFoundException
import com.quno.qunobackend.domain.vote.InvalidVoteValueException
import com.quno.qunobackend.domain.vote.SelfVoteException
import com.quno.qunobackend.domain.vote.VoteTargetType
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class CastVoteUseCaseTest {
    private val questionRepository = InMemoryQuestionRepository()
    private val answerRepository = InMemoryAnswerRepository()
    private val tagRepository = InMemoryTagRepository()
    private val createQuestionUseCase = CreateQuestionUseCase(
        questionRepository, InMemoryQuestionVersionRepository(), tagRepository, InMemoryQuestionTagRepository(tagRepository),
    )
    private val voteRepository = InMemoryVoteRepository()
    private val castVoteUseCase = CastVoteUseCase(questionRepository, answerRepository, voteRepository)

    private fun aQuestion(authorId: Long = 1L): Long = createQuestionUseCase.execute(
        CreateQuestionCommand(authorId = authorId, title = "t", body = "body", environment = null, logs = null),
    ).id

    private fun anAnswer(questionId: Long, authorId: Long = 2L): Long =
        requireNotNull(answerRepository.save(Answer.write(questionId, authorId, "answer body", 1)).id)

    @Test
    fun `casting an upvote on a question registers it`() {
        val questionId = aQuestion()

        castVoteUseCase.execute(CastVoteCommand(voterId = 10L, targetType = VoteTargetType.QUESTION, targetId = questionId, value = 1))

        assertEquals(1L, voteRepository.sumScore(VoteTargetType.QUESTION, questionId))
    }

    @Test
    fun `casting a downvote on an answer registers it`() {
        val questionId = aQuestion()
        val answerId = anAnswer(questionId)

        castVoteUseCase.execute(CastVoteCommand(voterId = 10L, targetType = VoteTargetType.ANSWER, targetId = answerId, value = -1))

        assertEquals(-1L, voteRepository.sumScore(VoteTargetType.ANSWER, answerId))
    }

    @Test
    fun `voting again with a different value replaces the previous vote`() {
        val questionId = aQuestion()
        castVoteUseCase.execute(CastVoteCommand(voterId = 10L, targetType = VoteTargetType.QUESTION, targetId = questionId, value = 1))

        castVoteUseCase.execute(CastVoteCommand(voterId = 10L, targetType = VoteTargetType.QUESTION, targetId = questionId, value = -1))

        assertEquals(-1L, voteRepository.sumScore(VoteTargetType.QUESTION, questionId))
    }

    @Test
    fun `two different voters both count toward the score`() {
        val questionId = aQuestion()
        castVoteUseCase.execute(CastVoteCommand(voterId = 10L, targetType = VoteTargetType.QUESTION, targetId = questionId, value = 1))

        castVoteUseCase.execute(CastVoteCommand(voterId = 11L, targetType = VoteTargetType.QUESTION, targetId = questionId, value = 1))

        assertEquals(2L, voteRepository.sumScore(VoteTargetType.QUESTION, questionId))
    }

    @Test
    fun `rejects voting on your own question`() {
        val questionId = aQuestion(authorId = 1L)

        assertFailsWith<SelfVoteException> {
            castVoteUseCase.execute(CastVoteCommand(voterId = 1L, targetType = VoteTargetType.QUESTION, targetId = questionId, value = 1))
        }
    }

    @Test
    fun `rejects voting on your own answer`() {
        val questionId = aQuestion()
        val answerId = anAnswer(questionId, authorId = 2L)

        assertFailsWith<SelfVoteException> {
            castVoteUseCase.execute(CastVoteCommand(voterId = 2L, targetType = VoteTargetType.ANSWER, targetId = answerId, value = 1))
        }
    }

    @Test
    fun `rejects a value other than 1 or -1`() {
        val questionId = aQuestion()

        assertFailsWith<InvalidVoteValueException> {
            castVoteUseCase.execute(CastVoteCommand(voterId = 10L, targetType = VoteTargetType.QUESTION, targetId = questionId, value = 5))
        }
    }

    @Test
    fun `rejects voting on a question that does not exist`() {
        assertFailsWith<QuestionNotFoundException> {
            castVoteUseCase.execute(CastVoteCommand(voterId = 10L, targetType = VoteTargetType.QUESTION, targetId = 999L, value = 1))
        }
    }
}
