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
    val role: Role,
    /** Opt-in, defaults to false (Phase 22, ADR-0034) — a request is refused rather than merely
     * hidden when this is false, matching the original brainstorm's "expert sets whether they
     * accept Direct Ask". */
    val acceptsDirectAsk: Boolean,
    val createdAt: Instant,
    val updatedAt: Instant,
) {
    fun updateDirectAskSettings(accepts: Boolean): User =
        User(id, email, nickname, passwordHash, isActive, role, accepts, createdAt, Instant.now())

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
                role = Role.USER,
                acceptsDirectAsk = false,
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
            role: Role,
            acceptsDirectAsk: Boolean,
            createdAt: Instant,
            updatedAt: Instant,
        ): User = User(id, email, nickname, passwordHash, isActive, role, acceptsDirectAsk, createdAt, updatedAt)
    }
}
