package com.quno.qunobackend.infrastructure.persistence.jpa.repository

import com.quno.qunobackend.infrastructure.persistence.jpa.entity.QuestionVersionJpaEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface QuestionVersionJpaRepository : JpaRepository<QuestionVersionJpaEntity, Long> {
    fun findByQuestionIdAndVersionNumber(questionId: Long, versionNumber: Int): QuestionVersionJpaEntity?
    fun findAllByQuestionIdOrderByVersionNumberAsc(questionId: Long): List<QuestionVersionJpaEntity>

    @Query(
        value = """
            SELECT question_id AS questionId, MAX(version_number) AS versionNumber FROM question_versions
            WHERE question_id IN (:questionIds)
            GROUP BY question_id
        """,
        nativeQuery = true,
    )
    fun findLatestVersionNumbers(@Param("questionIds") questionIds: List<Long>): List<LatestVersionRow>
}

interface LatestVersionRow {
    val questionId: Long
    val versionNumber: Int
}
