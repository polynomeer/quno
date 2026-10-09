package com.quno.qunobackend.infrastructure.persistence.jpa.repository

import com.quno.qunobackend.infrastructure.persistence.jpa.entity.AnswerJpaEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface AnswerJpaRepository : JpaRepository<AnswerJpaEntity, Long> {
    fun findByIdAndDeletedAtIsNull(id: Long): AnswerJpaEntity?
    fun findAllByQuestionIdAndDeletedAtIsNull(questionId: Long): List<AnswerJpaEntity>
    fun findByQuestionIdAndIsAcceptedTrueAndDeletedAtIsNull(questionId: Long): AnswerJpaEntity?
    fun findAllByAuthorIdAndDeletedAtIsNullOrderByCreatedAtDesc(authorId: Long): List<AnswerJpaEntity>

    @Query(
        value = """
            SELECT question_id AS questionId, COUNT(*) AS answerCount FROM answers
            WHERE question_id IN (:questionIds) AND deleted_at IS NULL
            GROUP BY question_id
        """,
        nativeQuery = true,
    )
    fun countByQuestionIds(@Param("questionIds") questionIds: List<Long>): List<AnswerCountRow>
}

interface AnswerCountRow {
    val questionId: Long
    val answerCount: Long
}
