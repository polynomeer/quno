package com.quno.qunobackend.application.follow.usecase

import com.quno.qunobackend.application.user.dto.SignUpCommand
import com.quno.qunobackend.application.user.usecase.InMemoryUserRepository
import com.quno.qunobackend.application.user.usecase.SignUpUseCase
import org.junit.jupiter.api.Test
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import kotlin.test.assertEquals

class ListMyFollowingUseCaseTest {
    private val userRepository = InMemoryUserRepository()
    private val signUpUseCase = SignUpUseCase(userRepository, BCryptPasswordEncoder())
    private val userFollowRepository = InMemoryUserFollowRepository()
    private val followUseCase = FollowUserUseCase(userRepository, userFollowRepository)
    private val listUseCase = ListMyFollowingUseCase(userFollowRepository, userRepository)

    private fun aUser(nickname: String): Long =
        signUpUseCase.execute(SignUpCommand("$nickname@example.com", nickname, "password123")).userId

    @Test
    fun `lists the users a user follows`() {
        val followerId = aUser("alice")
        val followeeId = aUser("bob")
        followUseCase.execute(followerId, followeeId)

        val result = listUseCase.execute(followerId)

        assertEquals(listOf("bob"), result.map { it.nickname })
    }

    @Test
    fun `returns nothing for a user following no one`() {
        assertEquals(emptyList(), listUseCase.execute(999L))
    }
}
