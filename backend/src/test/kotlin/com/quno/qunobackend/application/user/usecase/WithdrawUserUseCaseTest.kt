package com.quno.qunobackend.application.user.usecase

import com.quno.qunobackend.application.user.dto.SignUpCommand
import com.quno.qunobackend.application.user.dto.WithdrawUserCommand
import com.quno.qunobackend.domain.user.InvalidCredentialsException
import com.quno.qunobackend.domain.user.UserNotFoundException
import org.junit.jupiter.api.Test
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals

class WithdrawUserUseCaseTest {
    private val repository = InMemoryUserRepository()
    private val passwordEncoder = BCryptPasswordEncoder()
    private val signUpUseCase = SignUpUseCase(repository, passwordEncoder)
    private val useCase = WithdrawUserUseCase(repository, passwordEncoder)

    @Test
    fun `withdraws the account when the password is correct`() {
        val signedUp = signUpUseCase.execute(SignUpCommand(email = "a@b.com", nickname = "alice", rawPassword = "password123"))

        useCase.execute(WithdrawUserCommand(userId = signedUp.userId, rawPassword = "password123"))

        val withdrawn = repository.findById(signedUp.userId)!!
        assertFalse(withdrawn.isActive)
        assertNotEquals("a@b.com", withdrawn.email)
        assertNotEquals("alice", withdrawn.nickname)
    }

    @Test
    fun `rejects withdrawal with the wrong password`() {
        val signedUp = signUpUseCase.execute(SignUpCommand(email = "a@b.com", nickname = "alice", rawPassword = "password123"))

        assertFailsWith<InvalidCredentialsException> {
            useCase.execute(WithdrawUserCommand(userId = signedUp.userId, rawPassword = "wrong-password"))
        }
    }

    @Test
    fun `rejects withdrawal of a nonexistent user`() {
        assertFailsWith<UserNotFoundException> {
            useCase.execute(WithdrawUserCommand(userId = 999, rawPassword = "irrelevant"))
        }
    }
}
