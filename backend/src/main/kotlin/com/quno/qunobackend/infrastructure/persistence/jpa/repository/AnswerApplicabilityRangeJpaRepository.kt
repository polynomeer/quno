package com.quno.qunobackend.infrastructure.persistence.jpa.repository

import com.quno.qunobackend.infrastructure.persistence.jpa.entity.AnswerApplicabilityRangeJpaEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface AnswerApplicabilityRangeJpaRepository : JpaRepository<AnswerApplicabilityRangeJpaEntity, Long> {
    @Modifying
    @Query("DELETE FROM AnswerApplicabilityRangeJpaEntity r WHERE r.answerId = :answerId")
    fun deleteAllByAnswerId(@Param("answerId") answerId: Long)

    fun findAllByAnswerIdInOrderByIdAsc(answerIds: Collection<Long>): List<AnswerApplicabilityRangeJpaEntity>

    fun findAllByQuestionIdInOrderByIdAsc(questionIds: Collection<Long>): List<AnswerApplicabilityRangeJpaEntity>
}
