package com.quno.qunobackend.application.user.usecase

import com.quno.qunobackend.application.user.dto.SignUpCommand
import com.quno.qunobackend.domain.user.DuplicateEmailException
import com.quno.qunobackend.domain.user.DuplicateNicknameException
import org.junit.jupiter.api.Test
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotEquals

class SignUpUseCaseTest {
    private val repository = InMemoryUserRepository()
    private val useCase = SignUpUseCase(repository, BCryptPasswordEncoder())

    @Test
    fun `signs up a new user and hashes the password`() {
        val result = useCase.execute(SignUpCommand(email = "a@b.com", nickname = "alice", rawPassword = "password123"))

        assertEquals("a@b.com", result.email)
        assertEquals("alice", result.nickname)
        assertNotEquals("password123", repository.findByEmail("a@b.com")!!.passwordHash)
    }

    @Test
    fun `rejects a duplicate email`() {
        useCase.execute(SignUpCommand(email = "a@b.com", nickname = "alice", rawPassword = "password123"))

        assertFailsWith<DuplicateEmailException> {
            useCase.execute(SignUpCommand(email = "a@b.com", nickname = "bob", rawPassword = "password123"))
        }
    }

    @Test
    fun `rejects a duplicate nickname`() {
        useCase.execute(SignUpCommand(email = "a@b.com", nickname = "alice", rawPassword = "password123"))

        assertFailsWith<DuplicateNicknameException> {
            useCase.execute(SignUpCommand(email = "c@d.com", nickname = "alice", rawPassword = "password123"))
        }
    }
}
