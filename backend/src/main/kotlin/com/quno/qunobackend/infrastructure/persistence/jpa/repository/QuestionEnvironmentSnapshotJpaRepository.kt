package com.quno.qunobackend.infrastructure.persistence.jpa.repository

import com.quno.qunobackend.infrastructure.persistence.jpa.entity.QuestionEnvironmentSnapshotJpaEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.time.Instant

interface QuestionEnvironmentSnapshotJpaRepository : JpaRepository<QuestionEnvironmentSnapshotJpaEntity, Long> {
    fun findByQuestionId(questionId: Long): QuestionEnvironmentSnapshotJpaEntity?

    @Modifying
    @Query("DELETE FROM QuestionEnvironmentSnapshotJpaEntity s WHERE s.questionId = :questionId")
    fun deleteByQuestionId(@Param("questionId") questionId: Long)

    /** Public questions only: this aggregate has no viewer, like the other community counts (ADR-0085). */
    @Query(
        value = """
            SELECT g.fingerprint AS fingerprint, g.n AS questionCount, g.last_seen AS lastSeenAt, d.document\:\:text AS document
            FROM (
                SELECT s.fingerprint, COUNT(*) AS n, MAX(s.created_at) AS last_seen, MAX(s.id) AS latest_id
                FROM question_environment_snapshots s
                JOIN questions q ON q.id = s.question_id
                WHERE q.deleted_at IS NULL AND q.visibility = 'PUBLIC'
                GROUP BY s.fingerprint
                ORDER BY n DESC, last_seen DESC
                LIMIT :limit
            ) g
            JOIN question_environment_snapshots d ON d.id = g.latest_id
            ORDER BY g.n DESC, g.last_seen DESC
        """,
        nativeQuery = true,
    )
    fun findEnvironmentGroups(@Param("limit") limit: Int): List<EnvironmentGroupProjection>
}

interface EnvironmentGroupProjection {
    fun getFingerprint(): String
    fun getQuestionCount(): Long
    fun getLastSeenAt(): Instant
    fun getDocument(): String
}
