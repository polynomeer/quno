package com.quno.qunobackend.infrastructure.persistence.jpa.repository

import com.quno.qunobackend.infrastructure.persistence.jpa.entity.QuestionJpaEntity
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.Repository
import org.springframework.data.repository.query.Param

/**
 * Not a CRUD repository — only runs the Spike Detection aggregate query (PLAN.md 8.2). Compares
 * each tag's last-1-day question volume against its own 14-day baseline daily average. Tags
 * with fewer than 3 recent questions are excluded to avoid flagging noise on rare tags.
 */
interface SpikeDetectionJpaRepository : Repository<QuestionJpaEntity, Long> {

    @Query(
        value = """
            SELECT
                t.id AS id,
                t.name AS name,
                t.slug AS slug,
                COUNT(DISTINCT CASE WHEN q.created_at >= NOW() - INTERVAL '1 day' THEN q.id END) AS recentCount,
                (COUNT(DISTINCT CASE WHEN q.created_at >= NOW() - INTERVAL '15 days' AND q.created_at < NOW() - INTERVAL '1 day' THEN q.id END)::float8 / 14) AS baselineAveragePerDay,
                (COUNT(DISTINCT CASE WHEN q.created_at >= NOW() - INTERVAL '1 day' THEN q.id END)::float8 /
                 GREATEST(COUNT(DISTINCT CASE WHEN q.created_at >= NOW() - INTERVAL '15 days' AND q.created_at < NOW() - INTERVAL '1 day' THEN q.id END)::float8 / 14, 0.1)) AS spikeRatio
            FROM tags t
            JOIN question_tags qt ON qt.tag_id = t.id
            JOIN questions q ON q.id = qt.question_id
            WHERE t.deleted_at IS NULL AND q.deleted_at IS NULL AND q.created_at >= NOW() - INTERVAL '15 days'
            GROUP BY t.id, t.name, t.slug
            HAVING COUNT(DISTINCT CASE WHEN q.created_at >= NOW() - INTERVAL '1 day' THEN q.id END) >= 3
            ORDER BY spikeRatio DESC, recentCount DESC
            LIMIT :limit
        """,
        nativeQuery = true,
    )
    fun findSpikingTags(@Param("limit") limit: Int): List<TagSpikeProjection>
}
