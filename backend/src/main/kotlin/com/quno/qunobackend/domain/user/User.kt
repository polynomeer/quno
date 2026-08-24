package com.quno.qunobackend.domain.user

import java.time.Instant

/**
 * Aggregate root for a Quno account. Password hashing happens outside this class
 * (application layer, via PasswordEncoder); this class only ever holds the hash.
 */
class User private constructor(
    val id: Long?,
    val email: String,
    val nickname: String,
    val passwordHash: String,
    val isActive: Boolean,
    val createdAt: Instant,
    val updatedAt: Instant,
) {
    companion object {
        fun register(email: String, nickname: String, passwordHash: String): User {
            require(email.isNotBlank()) { "email must not be blank" }
            require(nickname.isNotBlank()) { "nickname must not be blank" }
            require(passwordHash.isNotBlank()) { "passwordHash must not be blank" }
            val now = Instant.now()
            return User(
                id = null,
                email = email,
                nickname = nickname,
                passwordHash = passwordHash,
                isActive = true,
                createdAt = now,
                updatedAt = now,
            )
        }

        fun reconstitute(
            id: Long,
            email: String,
            nickname: String,
            passwordHash: String,
            isActive: Boolean,
            createdAt: Instant,
            updatedAt: Instant,
        ): User = User(id, email, nickname, passwordHash, isActive, createdAt, updatedAt)
    }
}
