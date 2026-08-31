package com.quno.qunobackend.application.reputation.usecase

import com.quno.qunobackend.application.user.usecase.InMemoryUserRepository
import com.quno.qunobackend.application.user.usecase.SignUpUseCase
import com.quno.qunobackend.application.user.dto.SignUpCommand
import com.quno.qunobackend.domain.reputation.ReputationRepository
import com.quno.qunobackend.domain.reputation.UserReputation
import com.quno.qunobackend.domain.user.UserNotFoundException
import org.junit.jupiter.api.Test
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class GetUserReputationUseCaseTest {
    private val userRepository = InMemoryUserRepository()
    private val signUpUseCase = SignUpUseCase(userRepository, BCryptPasswordEncoder())

    @Test
    fun `computes the weighted score from the repository's raw counts`() {
        val userId = signUpUseCase.execute(SignUpCommand("a@b.com", "alice", "password123")).userId
        val reputationRepository = object : ReputationRepository {
            override fun compute(userId: Long): UserReputation =
                UserReputation(userId, questionCount = 3, answerCount = 4, acceptedAnswerCount = 2, superAnswerCount = 1, voteScoreReceived = 5)
        }
        val useCase = GetUserReputationUseCase(userRepository, reputationRepository)

        val result = useCase.execute(userId)

        // 3*1 + 4*2 + 2*15 + 1*10 + 5*1 = 3 + 8 + 30 + 10 + 5 = 56
        assertEquals(56, result.score)
    }

    @Test
    fun `rejects a user that does not exist`() {
        val reputationRepository = object : ReputationRepository {
            override fun compute(userId: Long): UserReputation = UserReputation(userId, 0, 0, 0, 0, 0)
        }
        val useCase = GetUserReputationUseCase(userRepository, reputationRepository)

        assertFailsWith<UserNotFoundException> { useCase.execute(999L) }
    }
}
