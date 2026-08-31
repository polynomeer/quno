package com.quno.qunobackend.infrastructure.persistence.jpa.repository

import com.quno.qunobackend.infrastructure.persistence.jpa.entity.AnswerVersionJpaEntity
import org.springframework.data.jpa.repository.JpaRepository

interface AnswerVersionJpaRepository : JpaRepository<AnswerVersionJpaEntity, Long> {
    fun findByAnswerIdAndVersionNumber(answerId: Long, versionNumber: Int): AnswerVersionJpaEntity?
    fun findAllByAnswerIdOrderByVersionNumberAsc(answerId: Long): List<AnswerVersionJpaEntity>
}
