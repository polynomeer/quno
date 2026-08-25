package com.quno.qunobackend.infrastructure.persistence.jpa.repository

import com.quno.qunobackend.infrastructure.persistence.jpa.entity.QuestionJpaEntity
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.Repository
import org.springframework.data.repository.query.Param

/**
 * Not a CRUD repository — only runs the two "derived signal" queries behind PLAN.md 10.1.
 * Neither needs a new event log: "reopened" compares question_versions against outbox_events
 * timestamps, and "recently super-answered" just reads question_clusters.updated_at.
 */
interface FlowJpaRepository : Repository<QuestionJpaEntity, Long> {

    @Query(
        value = """
            SELECT q.id
            FROM questions q
            JOIN (
                SELECT question_id, MAX(created_at) AS latest_version_at
                FROM question_versions
                GROUP BY question_id
            ) latest ON latest.question_id = q.id
            JOIN (
                SELECT aggregate_id AS question_id, MAX(created_at) AS outdated_at
                FROM outbox_events
                WHERE event_type = 'QUESTION_OUTDATED'
                GROUP BY aggregate_id
            ) outdated ON outdated.question_id = q.id
            WHERE q.deleted_at IS NULL
              AND latest.latest_version_at > outdated.outdated_at
            ORDER BY latest.latest_version_at DESC
            LIMIT :limit
        """,
        nativeQuery = true,
    )
    fun findRecentlyReopenedQuestionIds(@Param("limit") limit: Int): List<Long>

    @Query(
        value = """
            SELECT id FROM question_clusters
            WHERE representative_answer_id IS NOT NULL
            ORDER BY updated_at DESC
            LIMIT :limit
        """,
        nativeQuery = true,
    )
    fun findRecentlySuperAnsweredClusterIds(@Param("limit") limit: Int): List<Long>
}
