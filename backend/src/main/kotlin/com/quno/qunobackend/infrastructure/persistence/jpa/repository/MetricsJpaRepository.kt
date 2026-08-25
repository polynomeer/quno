package com.quno.qunobackend.infrastructure.persistence.jpa.repository

import com.quno.qunobackend.infrastructure.persistence.jpa.entity.QuestionJpaEntity
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.Repository

/** Not a CRUD repository — only runs the aggregate snapshot query behind GET /api/v1/metrics. */
interface MetricsJpaRepository : Repository<QuestionJpaEntity, Long> {

    @Query(
        value = """
            SELECT
                (SELECT COUNT(*) FROM questions WHERE deleted_at IS NULL) AS totalQuestions,
                (SELECT COUNT(DISTINCT qv.question_id)
                   FROM question_versions qv
                   JOIN questions q ON q.id = qv.question_id
                  WHERE qv.version_number >= 2 AND q.deleted_at IS NULL) AS revisedQuestions,
                (SELECT COUNT(DISTINCT a.question_id)
                   FROM answers a
                   JOIN questions q ON q.id = a.question_id
                  WHERE a.deleted_at IS NULL AND q.deleted_at IS NULL) AS answeredQuestions,
                (SELECT COUNT(*) FROM questions WHERE status = 'RESOLVED' AND deleted_at IS NULL) AS resolvedQuestions,
                (SELECT COUNT(DISTINCT w.question_id)
                   FROM watches w
                   JOIN questions q ON q.id = w.question_id
                  WHERE q.deleted_at IS NULL) AS watchedQuestions,
                (SELECT COUNT(DISTINCT t.question_id)
                   FROM (
                       SELECT aggregate_id AS question_id FROM outbox_events WHERE created_at >= NOW() - INTERVAL '7 days'
                       UNION
                       SELECT question_id FROM watches WHERE created_at >= NOW() - INTERVAL '7 days'
                   ) t
                   JOIN questions q ON q.id = t.question_id
                  WHERE q.deleted_at IS NULL) AS livingQuestions
        """,
        nativeQuery = true,
    )
    fun snapshot(): MetricsSnapshotProjection
}
