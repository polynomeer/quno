package com.quno.qunobackend.infrastructure.persistence.jpa.repository

import com.quno.qunobackend.infrastructure.persistence.jpa.entity.QuestionJpaEntity
import jakarta.persistence.LockModeType
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Lock
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface QuestionJpaRepository : JpaRepository<QuestionJpaEntity, Long> {
    fun findByIdAndDeletedAtIsNull(id: Long): QuestionJpaEntity?

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select q from QuestionJpaEntity q where q.id = :id and q.deletedAt is null")
    fun findByIdForUpdate(@Param("id") id: Long): QuestionJpaEntity?

    fun findAllByAuthorIdAndDeletedAtIsNullOrderByCreatedAtDesc(authorId: Long): List<QuestionJpaEntity>
}
