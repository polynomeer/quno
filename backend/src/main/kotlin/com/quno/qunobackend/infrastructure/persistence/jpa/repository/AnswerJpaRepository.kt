package com.quno.qunobackend.infrastructure.persistence.jpa.repository

import com.quno.qunobackend.infrastructure.persistence.jpa.entity.AnswerJpaEntity
import org.springframework.data.jpa.repository.JpaRepository

interface AnswerJpaRepository : JpaRepository<AnswerJpaEntity, Long> {
    fun findByIdAndDeletedAtIsNull(id: Long): AnswerJpaEntity?
    fun findAllByQuestionIdAndDeletedAtIsNull(questionId: Long): List<AnswerJpaEntity>
    fun findByQuestionIdAndIsAcceptedTrueAndDeletedAtIsNull(questionId: Long): AnswerJpaEntity?
    fun findAllByAuthorIdAndDeletedAtIsNullOrderByCreatedAtDesc(authorId: Long): List<AnswerJpaEntity>
}
