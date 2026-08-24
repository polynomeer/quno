package com.quno.qunobackend.infrastructure.persistence.jpa.repository

import com.quno.qunobackend.infrastructure.persistence.jpa.entity.QuestionJpaEntity
import org.springframework.data.jpa.repository.JpaRepository

interface QuestionJpaRepository : JpaRepository<QuestionJpaEntity, Long> {
    fun findByIdAndDeletedAtIsNull(id: Long): QuestionJpaEntity?
}
