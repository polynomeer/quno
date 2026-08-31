package com.quno.qunobackend.application.user.usecase

import com.quno.qunobackend.application.answer.dto.WriteAnswerCommand
import com.quno.qunobackend.application.answer.usecase.InMemoryAnswerRepository
import com.quno.qunobackend.application.answer.usecase.InMemoryAnswerVersionRepository
import com.quno.qunobackend.application.answer.usecase.WriteAnswerUseCase
import com.quno.qunobackend.application.common.AnswerResultAssembler
import com.quno.qunobackend.application.common.InMemoryOutboxEventRepository
import com.quno.qunobackend.application.common.QuestionSummaryHydrator
import com.quno.qunobackend.application.question.dto.CreateQuestionCommand
import com.quno.qunobackend.application.question.usecase.CreateQuestionUseCase
import com.quno.qunobackend.application.question.usecase.InMemoryQuestionRepository
import com.quno.qunobackend.application.question.usecase.InMemoryQuestionVersionRepository
import com.quno.qunobackend.application.tag.usecase.FollowTagUseCase
import com.quno.qunobackend.application.tag.usecase.InMemoryQuestionTagRepository
import com.quno.qunobackend.application.tag.usecase.InMemoryTagRepository
import com.quno.qunobackend.application.tag.usecase.InMemoryUserTagFollowRepository
import com.quno.qunobackend.application.user.dto.SignUpCommand
import com.quno.qunobackend.application.vote.usecase.InMemoryVoteRepository
import com.quno.qunobackend.domain.user.UserNotFoundException
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import org.junit.jupiter.api.Test
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder

class GetUserProfileUseCaseTest {
    private val userRepository = InMemoryUserRepository()
    private val questionRepository = InMemoryQuestionRepository()
    private val questionVersionRepository = InMemoryQuestionVersionRepository()
    private val tagRepository = InMemoryTagRepository()
    private val questionTagRepository = InMemoryQuestionTagRepository(tagRepository)
    private val answerRepository = InMemoryAnswerRepository()
    private val userTagFollowRepository = InMemoryUserTagFollowRepository()
    private val answerResultAssembler = AnswerResultAssembler(questionRepository, questionVersionRepository, InMemoryVoteRepository())

    private val signUpUseCase = SignUpUseCase(userRepository, BCryptPasswordEncoder())
    private val createQuestionUseCase = CreateQuestionUseCase(
        questionRepository, questionVersionRepository, tagRepository, questionTagRepository,
    )
    private val writeAnswerUseCase = WriteAnswerUseCase(
        questionRepository, questionVersionRepository, answerRepository, InMemoryAnswerVersionRepository(),
        InMemoryOutboxEventRepository(), answerResultAssembler,
    )
    private val followTagUseCase = FollowTagUseCase(tagRepository, userTagFollowRepository)
    private val useCase = GetUserProfileUseCase(
        userRepository,
        questionRepository,
        answerRepository,
        userTagFollowRepository,
        tagRepository,
        QuestionSummaryHydrator(questionRepository, questionTagRepository, InMemoryVoteRepository()),
        answerResultAssembler,
    )

    @Test
    fun `assembles authored questions, authored answers, and followed tags`() {
        val userId = signUpUseCase.execute(SignUpCommand("a@b.com", "alice", "password123")).userId
        val otherUserId = signUpUseCase.execute(SignUpCommand("c@d.com", "bob", "password123")).userId

        val ownQuestionId = createQuestionUseCase.execute(
            CreateQuestionCommand(userId, "Redis timeout", "body", null, null, tagNames = listOf("redis")),
        ).id
        val othersQuestionId = createQuestionUseCase.execute(
            CreateQuestionCommand(otherUserId, "Kafka lag", "body", null, null),
        ).id
        val answer = writeAnswerUseCase.execute(WriteAnswerCommand(othersQuestionId, userId, "Try this."))

        val tag = tagRepository.findBySlug("redis") ?: error("tag should exist")
        followTagUseCase.execute(userId, requireNotNull(tag.id))

        val result = useCase.execute(userId)

        assertEquals("alice", result.nickname)
        assertEquals(listOf(ownQuestionId), result.questions.map { it.id })
        assertEquals(listOf(answer.id), result.answers.map { it.id })
        assertEquals(listOf("redis"), result.followedTags.map { it.name })
    }

    @Test
    fun `throws when the user does not exist`() {
        assertFailsWith<UserNotFoundException> { useCase.execute(999L) }
    }
}
