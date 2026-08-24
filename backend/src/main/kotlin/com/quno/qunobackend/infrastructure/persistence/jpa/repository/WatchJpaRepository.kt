package com.quno.qunobackend.infrastructure.persistence.jpa.repository

import com.quno.qunobackend.infrastructure.persistence.jpa.entity.WatchId
import com.quno.qunobackend.infrastructure.persistence.jpa.entity.WatchJpaEntity
import org.springframework.data.jpa.repository.JpaRepository

interface WatchJpaRepository : JpaRepository<WatchJpaEntity, WatchId> {
    fun findAllByUserId(userId: Long): List<WatchJpaEntity>
    fun findAllByQuestionId(questionId: Long): List<WatchJpaEntity>
}
