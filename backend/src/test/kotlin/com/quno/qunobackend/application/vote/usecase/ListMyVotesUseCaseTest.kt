package com.quno.qunobackend.application.vote.usecase

import com.quno.qunobackend.application.answer.usecase.InMemoryAnswerRepository
import com.quno.qunobackend.application.question.dto.CreateQuestionCommand
import com.quno.qunobackend.application.question.usecase.CreateQuestionUseCase
import com.quno.qunobackend.application.question.usecase.InMemoryQuestionRepository
import com.quno.qunobackend.application.question.usecase.InMemoryQuestionVersionRepository
import com.quno.qunobackend.application.tag.usecase.InMemoryQuestionTagRepository
import com.quno.qunobackend.application.tag.usecase.InMemoryTagRepository
import com.quno.qunobackend.application.vote.dto.CastVoteCommand
import com.quno.qunobackend.application.vote.dto.VoteResult
import com.quno.qunobackend.domain.vote.VoteTargetType
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class ListMyVotesUseCaseTest {
    private val questionRepository = InMemoryQuestionRepository()
    private val answerRepository = InMemoryAnswerRepository()
    private val tagRepository = InMemoryTagRepository()
    private val createQuestionUseCase = CreateQuestionUseCase(
        questionRepository, InMemoryQuestionVersionRepository(), tagRepository, InMemoryQuestionTagRepository(tagRepository),
    )
    private val voteRepository = InMemoryVoteRepository()
    private val castVoteUseCase = CastVoteUseCase(questionRepository, answerRepository, voteRepository)
    private val listMyVotesUseCase = ListMyVotesUseCase(voteRepository)

    @Test
    fun `lists every vote a user has cast`() {
        val questionId = createQuestionUseCase.execute(
            CreateQuestionCommand(authorId = 1L, title = "t", body = "body", environment = null, logs = null),
        ).id
        castVoteUseCase.execute(CastVoteCommand(voterId = 10L, targetType = VoteTargetType.QUESTION, targetId = questionId, value = 1))

        val result = listMyVotesUseCase.execute(voterId = 10L)

        assertEquals(listOf(VoteResult(VoteTargetType.QUESTION, questionId, 1)), result)
    }

    @Test
    fun `returns nothing for a user who never voted`() {
        assertEquals(emptyList(), listMyVotesUseCase.execute(voterId = 999L))
    }
}
