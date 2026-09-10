package com.quno.qunobackend.domain.user

import org.junit.jupiter.api.Test
import java.time.Instant
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class UserTest {

    @Test
    fun `register creates an active user without an id yet`() {
        val user = User.register(email = "a@b.com", nickname = "alice", passwordHash = "hashed")

        assertTrue(user.isActive)
        assertEquals("a@b.com", user.email)
        assertEquals("alice", user.nickname)
        assertNull(user.id)
    }

    @Test
    fun `register rejects a blank email`() {
        assertFailsWith<IllegalArgumentException> {
            User.register(email = " ", nickname = "alice", passwordHash = "hashed")
        }
    }

    @Test
    fun `register rejects a blank nickname`() {
        assertFailsWith<IllegalArgumentException> {
            User.register(email = "a@b.com", nickname = " ", passwordHash = "hashed")
        }
    }

    @Test
    fun `withdraw anonymizes PII and deactivates the account`() {
        val user = User.reconstitute(
            id = 42,
            email = "real-person@example.com",
            nickname = "realNickname",
            passwordHash = "originalHash",
            isActive = true,
            role = Role.USER,
            acceptsDirectAsk = true,
            createdAt = Instant.parse("2026-01-01T00:00:00Z"),
            updatedAt = Instant.parse("2026-01-01T00:00:00Z"),
        )

        val withdrawn = user.withdraw(randomPasswordHash = "newRandomHash")

        assertEquals(42, withdrawn.id)
        assertTrue(withdrawn.email.contains("42"))
        assertNotEquals("real-person@example.com", withdrawn.email)
        assertTrue(withdrawn.nickname.contains("42"))
        assertNotEquals("realNickname", withdrawn.nickname)
        assertEquals("newRandomHash", withdrawn.passwordHash)
        assertTrue(!withdrawn.isActive)
        assertTrue(!withdrawn.acceptsDirectAsk)
        assertEquals(user.createdAt, withdrawn.createdAt)
    }

    @Test
    fun `withdraw rejects an unpersisted user`() {
        val user = User.register(email = "a@b.com", nickname = "alice", passwordHash = "hashed")

        assertFailsWith<IllegalArgumentException> {
            user.withdraw(randomPasswordHash = "irrelevant")
        }
    }
}
