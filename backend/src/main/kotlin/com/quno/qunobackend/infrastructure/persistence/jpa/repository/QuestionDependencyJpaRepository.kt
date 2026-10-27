package com.quno.qunobackend.infrastructure.persistence.jpa.repository

import com.quno.qunobackend.infrastructure.persistence.jpa.entity.QuestionDependencyJpaEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface QuestionDependencyJpaRepository : JpaRepository<QuestionDependencyJpaEntity, Long> {
    @Modifying
    @Query("DELETE FROM QuestionDependencyJpaEntity d WHERE d.questionId = :questionId")
    fun deleteAllByQuestionId(@Param("questionId") questionId: Long)

    fun findAllByQuestionIdOrderByIdAsc(questionId: Long): List<QuestionDependencyJpaEntity>

    fun findAllByQuestionIdInOrderByIdAsc(questionIds: Collection<Long>): List<QuestionDependencyJpaEntity>
}
