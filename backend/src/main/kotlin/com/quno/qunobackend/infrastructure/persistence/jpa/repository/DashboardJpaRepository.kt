package com.quno.qunobackend.infrastructure.persistence.jpa.repository

import com.quno.qunobackend.infrastructure.persistence.jpa.entity.QuestionJpaEntity
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.Repository
import org.springframework.data.repository.query.Param

/** Not a CRUD repository — only runs the dashboard's aggregate ranking queries. */
interface DashboardJpaRepository : Repository<QuestionJpaEntity, Long> {

    @Query(
        value = """
            SELECT q.id
            FROM questions q
            LEFT JOIN (
                SELECT question_id, COUNT(*) AS watch_count FROM watches GROUP BY question_id
            ) w ON w.question_id = q.id
            LEFT JOIN (
                SELECT question_id, COUNT(*) AS answer_count FROM answers WHERE deleted_at IS NULL GROUP BY question_id
            ) a ON a.question_id = q.id
            WHERE q.deleted_at IS NULL
            ORDER BY (COALESCE(w.watch_count, 0) * 3 + COALESCE(a.answer_count, 0) * 2) DESC, q.created_at DESC
            LIMIT :limit
        """,
        nativeQuery = true,
    )
    fun findPopularQuestionIds(@Param("limit") limit: Int): List<Long>

    @Query(
        value = """
            SELECT t.id AS id, t.name AS name, t.slug AS slug, COUNT(DISTINCT qt.question_id) AS questionCount
            FROM tags t
            JOIN question_tags qt ON qt.tag_id = t.id
            JOIN questions q ON q.id = qt.question_id
            WHERE t.deleted_at IS NULL
              AND q.deleted_at IS NULL
              AND q.created_at >= NOW() - INTERVAL '7 days'
            GROUP BY t.id, t.name, t.slug
            ORDER BY questionCount DESC, t.id DESC
            LIMIT :limit
        """,
        nativeQuery = true,
    )
    fun findTrendingTags(@Param("limit") limit: Int): List<TagTrendProjection>

    @Query(
        value = """
            SELECT id FROM questions
            WHERE status = 'RESOLVED' AND deleted_at IS NULL AND updated_at >= date_trunc('day', NOW())
            ORDER BY updated_at DESC
            LIMIT :limit
        """,
        nativeQuery = true,
    )
    fun findResolvedTodayQuestionIds(@Param("limit") limit: Int): List<Long>
}
