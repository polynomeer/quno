package com.quno.qunobackend.infrastructure.persistence.jpa.repository

import com.quno.qunobackend.infrastructure.persistence.jpa.entity.QuestionEnvironmentSnapshotJpaEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface QuestionEnvironmentSnapshotJpaRepository : JpaRepository<QuestionEnvironmentSnapshotJpaEntity, Long> {
    fun findByQuestionId(questionId: Long): QuestionEnvironmentSnapshotJpaEntity?

    @Modifying
    @Query("DELETE FROM QuestionEnvironmentSnapshotJpaEntity s WHERE s.questionId = :questionId")
    fun deleteByQuestionId(@Param("questionId") questionId: Long)
}
