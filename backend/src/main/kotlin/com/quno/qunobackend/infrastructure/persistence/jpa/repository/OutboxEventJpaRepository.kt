package com.quno.qunobackend.infrastructure.persistence.jpa.repository

import com.quno.qunobackend.infrastructure.persistence.jpa.entity.OutboxEventJpaEntity
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.time.Instant

interface OutboxEventJpaRepository : JpaRepository<OutboxEventJpaEntity, Long> {
    fun findAllByPublishedAtIsNullOrderByCreatedAtAsc(pageable: Pageable): List<OutboxEventJpaEntity>

    @Modifying(clearAutomatically = true)
    @Query("update OutboxEventJpaEntity e set e.publishedAt = :publishedAt where e.id = :id")
    fun markPublished(@Param("id") id: Long, @Param("publishedAt") publishedAt: Instant): Int
}
