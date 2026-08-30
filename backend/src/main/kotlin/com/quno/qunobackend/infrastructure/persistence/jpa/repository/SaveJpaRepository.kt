package com.quno.qunobackend.infrastructure.persistence.jpa.repository

import com.quno.qunobackend.infrastructure.persistence.jpa.entity.SaveId
import com.quno.qunobackend.infrastructure.persistence.jpa.entity.SaveJpaEntity
import org.springframework.data.jpa.repository.JpaRepository

interface SaveJpaRepository : JpaRepository<SaveJpaEntity, SaveId> {
    fun findAllByUserId(userId: Long): List<SaveJpaEntity>
}
