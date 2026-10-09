package com.quno.qunobackend.infrastructure.persistence.jpa.repository

import com.quno.qunobackend.infrastructure.persistence.jpa.entity.QuestionViewCountJpaEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface QuestionViewCountJpaRepository : JpaRepository<QuestionViewCountJpaEntity, Long> {
    /** Upsert in one statement — no read-modify-write race between concurrent viewers. */
    @Modifying
    @Query(
        value = """
            INSERT INTO question_view_counts (question_id, view_count) VALUES (:questionId, 1)
            ON CONFLICT (question_id) DO UPDATE SET view_count = question_view_counts.view_count + 1
        """,
        nativeQuery = true,
    )
    fun increment(@Param("questionId") questionId: Long)

    fun findAllByQuestionIdIn(questionIds: List<Long>): List<QuestionViewCountJpaEntity>
}
