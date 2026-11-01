package com.quno.qunobackend.application.token.usecase

import com.quno.qunobackend.domain.token.InvalidPersonalAccessTokenRequestException
import com.quno.qunobackend.domain.token.PersonalAccessToken
import com.quno.qunobackend.domain.token.PersonalAccessTokenLimitExceededException
import com.quno.qunobackend.domain.token.PersonalAccessTokenNotFoundException
import com.quno.qunobackend.domain.token.PersonalAccessTokenRepository
import com.quno.qunobackend.domain.token.TokenScope
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.security.MessageDigest
import java.security.SecureRandom
import java.time.Duration
import java.time.Instant
import java.util.Base64

/** Shared by issuing and authenticating: tokens are looked up by the hex SHA-256 of the whole secret (ADR-0079). */
internal fun hashOf(secret: String): String = MessageDigest.getInstance("SHA-256").digest(secret.toByteArray()).joinToString("") { "%02x".format(it) }

/** The only time the secret exists outside the caller's hands. */
data class IssuedPersonalAccessToken(val secret: String, val token: PersonalAccessToken)

/** Who a token speaks for, as the security filter needs it. */
data class TokenPrincipal(val tokenId: Long, val userId: Long, val scopes: Set<TokenScope>)

@Service
class IssuePersonalAccessTokenUseCase(
    private val repository: PersonalAccessTokenRepository,
) {
    private val random = SecureRandom()

    @Transactional
    fun execute(userId: Long, name: String, scopes: Set<TokenScope>, lifetimeDays: Long): IssuedPersonalAccessToken {
        val trimmed = name.trim()
        if (trimmed.isEmpty() || trimmed.length > PersonalAccessToken.MAX_NAME_LENGTH) {
            throw InvalidPersonalAccessTokenRequestException("name must be 1-${PersonalAccessToken.MAX_NAME_LENGTH} characters")
        }
        if (scopes.isEmpty()) throw InvalidPersonalAccessTokenRequestException("choose at least one scope")
        if (lifetimeDays !in PersonalAccessToken.ALLOWED_LIFETIME_DAYS) {
            throw InvalidPersonalAccessTokenRequestException("lifetimeDays must be one of ${PersonalAccessToken.ALLOWED_LIFETIME_DAYS.sorted()}")
        }
        val now = Instant.now()
        if (repository.countActiveByUserId(userId, now) >= PersonalAccessToken.MAX_ACTIVE_PER_USER) {
            throw PersonalAccessTokenLimitExceededException(PersonalAccessToken.MAX_ACTIVE_PER_USER)
        }

        val bytes = ByteArray(32).also(random::nextBytes)
        val secret = PersonalAccessToken.SECRET_PREFIX + Base64.getUrlEncoder().withoutPadding().encodeToString(bytes)
        val token = repository.save(
            PersonalAccessToken(
                id = null,
                userId = userId,
                name = trimmed,
                tokenHash = hashOf(secret),
                displayPrefix = secret.take(DISPLAY_PREFIX_LENGTH),
                scopes = scopes,
                createdAt = now,
                expiresAt = now.plus(Duration.ofDays(lifetimeDays)),
                lastUsedAt = null,
                revokedAt = null,
            ),
        )
        return IssuedPersonalAccessToken(secret, token)
    }

    companion object {
        private const val DISPLAY_PREFIX_LENGTH = 12
    }
}

@Service
class ManagePersonalAccessTokensUseCase(
    private val repository: PersonalAccessTokenRepository,
) {
    fun list(userId: Long): List<PersonalAccessToken> = repository.findAllByUserId(userId)

    /** Someone else's token is reported as missing, never as forbidden, so ids can't be probed. Idempotent. */
    @Transactional
    fun revoke(userId: Long, tokenId: Long) {
        val token = repository.findById(tokenId)?.takeIf { it.userId == userId } ?: throw PersonalAccessTokenNotFoundException(tokenId)
        if (token.revokedAt == null) repository.save(token.copy(revokedAt = Instant.now()))
    }
}

/** Called by the security filter for every `quno_pat_` bearer token; null means "not a valid token". */
@Service
class AuthenticatePersonalAccessTokenUseCase(
    private val repository: PersonalAccessTokenRepository,
) {
    fun execute(secret: String): TokenPrincipal? {
        if (!secret.startsWith(PersonalAccessToken.SECRET_PREFIX)) return null
        val now = Instant.now()
        val token = repository.findByTokenHash(hashOf(secret))?.takeIf { it.isActive(now) } ?: return null
        val id = requireNotNull(token.id)
        if (token.needsLastUsedTouch(now)) repository.touchLastUsed(id, now)
        return TokenPrincipal(tokenId = id, userId = token.userId, scopes = token.scopes)
    }
}
