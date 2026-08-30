package com.quno.qunobackend.application.follow.usecase

import com.quno.qunobackend.application.user.dto.SignUpCommand
import com.quno.qunobackend.application.user.usecase.InMemoryUserRepository
import com.quno.qunobackend.application.user.usecase.SignUpUseCase
import com.quno.qunobackend.domain.follow.SelfFollowException
import com.quno.qunobackend.domain.user.UserNotFoundException
import org.junit.jupiter.api.Test
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class FollowUserUseCaseTest {
    private val userRepository = InMemoryUserRepository()
    private val signUpUseCase = SignUpUseCase(userRepository, BCryptPasswordEncoder())
    private val userFollowRepository = InMemoryUserFollowRepository()
    private val followUseCase = FollowUserUseCase(userRepository, userFollowRepository)
    private val unfollowUseCase = UnfollowUserUseCase(userFollowRepository)

    private fun aUser(nickname: String): Long =
        signUpUseCase.execute(SignUpCommand("$nickname@example.com", nickname, "password123")).userId

    @Test
    fun `following an existing user registers the follow`() {
        val followerId = aUser("alice")
        val followeeId = aUser("bob")

        followUseCase.execute(followerId, followeeId)

        assertTrue(userFollowRepository.isFollowing(followerId, followeeId))
    }

    @Test
    fun `following twice stays idempotent`() {
        val followerId = aUser("alice")
        val followeeId = aUser("bob")

        followUseCase.execute(followerId, followeeId)
        followUseCase.execute(followerId, followeeId)

        assertTrue(userFollowRepository.findFolloweeIds(followerId).size == 1)
    }

    @Test
    fun `rejects following yourself`() {
        val userId = aUser("alice")

        assertFailsWith<SelfFollowException> { followUseCase.execute(userId, userId) }
    }

    @Test
    fun `rejects following a user that does not exist`() {
        val followerId = aUser("alice")

        assertFailsWith<UserNotFoundException> { followUseCase.execute(followerId, 999L) }
    }

    @Test
    fun `unfollow clears the follow`() {
        val followerId = aUser("alice")
        val followeeId = aUser("bob")
        followUseCase.execute(followerId, followeeId)

        unfollowUseCase.execute(followerId, followeeId)

        assertFalse(userFollowRepository.isFollowing(followerId, followeeId))
    }
}
