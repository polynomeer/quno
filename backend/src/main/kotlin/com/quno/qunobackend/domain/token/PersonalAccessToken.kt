package com.quno.qunobackend.domain.token

import java.time.Duration
import java.time.Instant

/** What a personal access token may do (Phase 65.1, ADR-0079). Requests outside every granted scope are a 403. */
enum class TokenScope {
    /** Every GET, plus the preview POSTs that store nothing. */
    READ,

    /** Asking and revising questions. */
    ASK,
}

class PersonalAccessTokenLimitExceededException(limit: Int) : RuntimeException("you can keep at most $limit active tokens")

class PersonalAccessTokenNotFoundException(id: Long) : RuntimeException("token $id not found")

class InvalidPersonalAccessTokenRequestException(message: String) : RuntimeException(message)

/**
 * A token for the CLI and MCP server (ADR-0079). Only the SHA-256 of the secret is kept;
 * [displayPrefix] is enough for a person to tell their tokens apart.
 */
data class PersonalAccessToken(
    val id: Long?,
    val userId: Long,
    val name: String,
    val tokenHash: String,
    val displayPrefix: String,
    val scopes: Set<TokenScope>,
    val createdAt: Instant,
    val expiresAt: Instant,
    val lastUsedAt: Instant?,
    val revokedAt: Instant?,
) {
    fun isActive(now: Instant = Instant.now()): Boolean = revokedAt == null && now.isBefore(expiresAt)

    /** Writing on every request would turn reads into writes; once per [LAST_USED_GRANULARITY] is enough to show. */
    fun needsLastUsedTouch(now: Instant): Boolean = lastUsedAt == null || Duration.between(lastUsedAt, now) >= LAST_USED_GRANULARITY

    companion object {
        const val SECRET_PREFIX = "quno_pat_"
        const val MAX_ACTIVE_PER_USER = 10
        const val MAX_NAME_LENGTH = 100
        val ALLOWED_LIFETIME_DAYS = setOf(30L, 90L, 365L)
        val LAST_USED_GRANULARITY: Duration = Duration.ofMinutes(5)
    }
}

/** Port implemented by infrastructure/persistence/jpa/adapter/PersonalAccessTokenRepositoryAdapter. */
interface PersonalAccessTokenRepository {
    fun save(token: PersonalAccessToken): PersonalAccessToken

    fun findByTokenHash(tokenHash: String): PersonalAccessToken?

    fun findById(id: Long): PersonalAccessToken?

    /** Newest first, revoked and expired ones included so the user can see what happened. */
    fun findAllByUserId(userId: Long): List<PersonalAccessToken>

    fun countActiveByUserId(userId: Long, now: Instant): Int

    fun touchLastUsed(id: Long, at: Instant)
}
