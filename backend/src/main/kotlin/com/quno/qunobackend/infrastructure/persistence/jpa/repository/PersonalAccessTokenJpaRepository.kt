package com.quno.qunobackend.infrastructure.persistence.jpa.repository

import com.quno.qunobackend.infrastructure.persistence.jpa.entity.PersonalAccessTokenJpaEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.transaction.annotation.Transactional
import java.time.Instant

interface PersonalAccessTokenJpaRepository : JpaRepository<PersonalAccessTokenJpaEntity, Long> {
    fun findByTokenHash(tokenHash: String): PersonalAccessTokenJpaEntity?

    fun findAllByUserIdOrderByIdDesc(userId: Long): List<PersonalAccessTokenJpaEntity>

    @Query("SELECT COUNT(t) FROM PersonalAccessTokenJpaEntity t WHERE t.userId = :userId AND t.revokedAt IS NULL AND t.expiresAt > :now")
    fun countActive(@Param("userId") userId: Long, @Param("now") now: Instant): Long

    /** Runs on authentication outside any request transaction, so it carries its own. */
    @Transactional
    @Modifying
    @Query("UPDATE PersonalAccessTokenJpaEntity t SET t.lastUsedAt = :at WHERE t.id = :id")
    fun touchLastUsed(@Param("id") id: Long, @Param("at") at: Instant)
}
