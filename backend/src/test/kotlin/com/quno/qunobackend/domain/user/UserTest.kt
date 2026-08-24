package com.quno.qunobackend.domain.user

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
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
}
