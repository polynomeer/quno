package com.quno.qunobackend.infrastructure.persistence.jpa.adapter

import com.quno.qunobackend.domain.token.PersonalAccessToken
import com.quno.qunobackend.domain.token.PersonalAccessTokenRepository
import com.quno.qunobackend.domain.token.TokenScope
import com.quno.qunobackend.infrastructure.persistence.jpa.entity.PersonalAccessTokenJpaEntity
import com.quno.qunobackend.infrastructure.persistence.jpa.repository.PersonalAccessTokenJpaRepository
import org.springframework.stereotype.Component
import java.time.Instant

@Component
class PersonalAccessTokenRepositoryAdapter(
    private val jpaRepository: PersonalAccessTokenJpaRepository,
) : PersonalAccessTokenRepository {

    override fun save(token: PersonalAccessToken): PersonalAccessToken = jpaRepository.save(
        PersonalAccessTokenJpaEntity(
            id = token.id,
            userId = token.userId,
            name = token.name,
            tokenHash = token.tokenHash,
            displayPrefix = token.displayPrefix,
            scopes = token.scopes.sorted().joinToString(",") { it.name },
            createdAt = token.createdAt,
            expiresAt = token.expiresAt,
            lastUsedAt = token.lastUsedAt,
            revokedAt = token.revokedAt,
        ),
    ).toDomain()

    override fun findByTokenHash(tokenHash: String): PersonalAccessToken? = jpaRepository.findByTokenHash(tokenHash)?.toDomain()

    override fun findById(id: Long): PersonalAccessToken? = jpaRepository.findById(id).orElse(null)?.toDomain()

    override fun findAllByUserId(userId: Long): List<PersonalAccessToken> = jpaRepository.findAllByUserIdOrderByIdDesc(userId).map { it.toDomain() }

    override fun countActiveByUserId(userId: Long, now: Instant): Int = jpaRepository.countActive(userId, now).toInt()

    override fun touchLastUsed(id: Long, at: Instant) = jpaRepository.touchLastUsed(id, at)

    private fun PersonalAccessTokenJpaEntity.toDomain() = PersonalAccessToken(
        id = id,
        userId = userId,
        name = name,
        tokenHash = tokenHash,
        displayPrefix = displayPrefix,
        scopes = scopes.split(",").filter { it.isNotBlank() }.map { TokenScope.valueOf(it) }.toSet(),
        createdAt = createdAt,
        expiresAt = expiresAt,
        lastUsedAt = lastUsedAt,
        revokedAt = revokedAt,
    )
}
