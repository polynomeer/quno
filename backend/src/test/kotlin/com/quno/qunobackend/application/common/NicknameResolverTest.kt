package com.quno.qunobackend.application.common

import com.quno.qunobackend.application.user.usecase.InMemoryUserRepository
import com.quno.qunobackend.domain.user.User
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class NicknameResolverTest {
    private val userRepository = InMemoryUserRepository()
    private val resolver = NicknameResolver(userRepository)

    @Test
    fun `resolves known ids, skips nulls and unknown ids`() {
        val user = userRepository.save(User.register(email = "nick@example.com", nickname = "devGuru", passwordHash = "hash"))
        val id = requireNotNull(user.id)

        assertEquals(mapOf(id to "devGuru"), resolver.nicknamesOf(listOf(id, null, 999L, id)))
    }

    @Test
    fun `returns an empty map for no ids`() {
        assertEquals(emptyMap(), resolver.nicknamesOf(emptyList()))
    }
}
