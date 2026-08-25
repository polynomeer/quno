package com.quno.qunobackend.infrastructure.persistence.jpa.repository

import com.quno.qunobackend.infrastructure.persistence.jpa.entity.ReviewRequestJpaEntity
import org.springframework.data.jpa.repository.JpaRepository

interface ReviewRequestJpaRepository : JpaRepository<ReviewRequestJpaEntity, Long> {
    fun findAllByQuestionIdOrderByCreatedAtDesc(questionId: Long): List<ReviewRequestJpaEntity>
}
