package com.quno.qunobackend.application.badge.usecase

import com.quno.qunobackend.application.user.dto.SignUpCommand
import com.quno.qunobackend.application.user.usecase.InMemoryUserRepository
import com.quno.qunobackend.application.user.usecase.SignUpUseCase
import com.quno.qunobackend.domain.badge.BadgeRepository
import com.quno.qunobackend.domain.badge.BadgeType
import com.quno.qunobackend.domain.reputation.ReputationRepository
import com.quno.qunobackend.domain.reputation.UserReputation
import com.quno.qunobackend.domain.user.UserNotFoundException
import org.junit.jupiter.api.Test
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class GetUserBadgesUseCaseTest {
    private val userRepository = InMemoryUserRepository()
    private val signUpUseCase = SignUpUseCase(userRepository, BCryptPasswordEncoder())

    private fun aUser(): Long = signUpUseCase.execute(SignUpCommand("a@b.com", "alice", "password123")).userId

    private fun useCase(
        questionCount: Long = 0,
        answerCount: Long = 0,
        acceptedAnswerCount: Long = 0,
        superAnswerCount: Long = 0,
        voteScoreReceived: Long = 0,
    ): GetUserBadgesUseCase {
        val reputationRepository = object : ReputationRepository {
            override fun compute(userId: Long): UserReputation =
                UserReputation(userId, questionCount, answerCount, acceptedAnswerCount, superAnswerCount, voteScoreReceived = 0)
        }
        val badgeRepository = object : BadgeRepository {
            override fun sumVoteScoreReceived(userId: Long): Long = voteScoreReceived
        }
        return GetUserBadgesUseCase(userRepository, reputationRepository, badgeRepository)
    }

    @Test
    fun `a user with no activity earns no badges`() {
        val userId = aUser()

        assertEquals(emptyList(), useCase().execute(userId))
    }

    @Test
    fun `first question earns FIRST_QUESTION but not FIRST_ANSWER`() {
        val userId = aUser()

        assertEquals(listOf(BadgeType.FIRST_QUESTION), useCase(questionCount = 1).execute(userId))
    }

    @Test
    fun `first answer earns FIRST_ANSWER`() {
        val userId = aUser()

        assertEquals(listOf(BadgeType.FIRST_ANSWER), useCase(answerCount = 1).execute(userId))
    }

    @Test
    fun `4 accepted answers is not enough for PROBLEM_SOLVER, 5 is`() {
        val userId = aUser()

        assertEquals(emptyList(), useCase(acceptedAnswerCount = 4).execute(userId))
        assertEquals(listOf(BadgeType.PROBLEM_SOLVER), useCase(acceptedAnswerCount = 5).execute(userId))
    }

    @Test
    fun `20 accepted answers earns both PROBLEM_SOLVER and TRUSTED_ANSWERER`() {
        val userId = aUser()

        assertEquals(
            listOf(BadgeType.PROBLEM_SOLVER, BadgeType.TRUSTED_ANSWERER),
            useCase(acceptedAnswerCount = 20).execute(userId),
        )
    }

    @Test
    fun `49 vote score is not enough for WELL_RECEIVED, 50 is`() {
        val userId = aUser()

        assertEquals(emptyList(), useCase(voteScoreReceived = 49).execute(userId))
        assertEquals(listOf(BadgeType.WELL_RECEIVED), useCase(voteScoreReceived = 50).execute(userId))
    }

    @Test
    fun `one Super Answer designation earns SUPER_ANSWER`() {
        val userId = aUser()

        assertEquals(listOf(BadgeType.SUPER_ANSWER), useCase(superAnswerCount = 1).execute(userId))
    }

    @Test
    fun `all six badges can be earned at once`() {
        val userId = aUser()

        val result = useCase(
            questionCount = 1,
            answerCount = 1,
            acceptedAnswerCount = 20,
            superAnswerCount = 1,
            voteScoreReceived = 50,
        ).execute(userId)

        assertEquals(BadgeType.entries, result)
    }

    @Test
    fun `rejects a user that does not exist`() {
        assertFailsWith<UserNotFoundException> { useCase().execute(999L) }
    }
}
