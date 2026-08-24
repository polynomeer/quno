package com.quno.qunobackend.infrastructure.persistence.jpa.repository

import com.quno.qunobackend.infrastructure.persistence.jpa.entity.NotificationJpaEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface NotificationJpaRepository : JpaRepository<NotificationJpaEntity, Long> {
    fun findAllByUserIdOrderByCreatedAtDesc(userId: Long): List<NotificationJpaEntity>

    @Modifying(clearAutomatically = true)
    @Query("update NotificationJpaEntity n set n.isRead = true where n.userId = :userId and n.isRead = false")
    fun markAllReadForUser(@Param("userId") userId: Long): Int
}
