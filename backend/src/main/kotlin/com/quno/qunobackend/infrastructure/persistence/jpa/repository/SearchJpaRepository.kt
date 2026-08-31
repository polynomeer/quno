package com.quno.qunobackend.infrastructure.persistence.jpa.repository

import com.quno.qunobackend.infrastructure.persistence.jpa.entity.QuestionJpaEntity
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.Repository
import org.springframework.data.repository.query.Param

/**
 * Not a CRUD repository (extends the bare marker `Repository`, not `JpaRepository`) — this
 * exists only to run the two cross-table native queries search needs.
 */
interface SearchJpaRepository : Repository<QuestionJpaEntity, Long> {

    @Query(
        value = """
            SELECT DISTINCT q.id
            FROM questions q
            JOIN question_versions v ON v.id = q.latest_version_id
            LEFT JOIN question_tags qt ON qt.question_id = q.id
            LEFT JOIN tags t ON t.id = qt.tag_id AND t.deleted_at IS NULL
            WHERE q.deleted_at IS NULL
              AND (
                to_tsvector('simple', v.title || ' ' || v.body_markdown || ' ' || coalesce(v.logs, ''))
                  @@ plainto_tsquery('simple', :query)
                OR t.name ILIKE CONCAT('%', :query, '%')
              )
            ORDER BY q.id DESC
            LIMIT :limit
        """,
        nativeQuery = true,
    )
    fun searchQuestionIdsByRelevance(@Param("query") query: String, @Param("limit") limit: Int): List<Long>

    /** Same candidate set as [searchQuestionIdsByRelevance], ordered by net vote score instead
     * (Phase 20, ADR-0032). */
    @Query(
        value = """
            SELECT q.id
            FROM questions q
            JOIN question_versions v ON v.id = q.latest_version_id
            LEFT JOIN question_tags qt ON qt.question_id = q.id
            LEFT JOIN tags t ON t.id = qt.tag_id AND t.deleted_at IS NULL
            LEFT JOIN (
                SELECT target_id, SUM(value) AS vote_score FROM votes WHERE target_type = 'QUESTION' GROUP BY target_id
            ) sv ON sv.target_id = q.id
            WHERE q.deleted_at IS NULL
              AND (
                to_tsvector('simple', v.title || ' ' || v.body_markdown || ' ' || coalesce(v.logs, ''))
                  @@ plainto_tsquery('simple', :query)
                OR t.name ILIKE CONCAT('%', :query, '%')
              )
            GROUP BY q.id, sv.vote_score
            ORDER BY COALESCE(sv.vote_score, 0) DESC, q.id DESC
            LIMIT :limit
        """,
        nativeQuery = true,
    )
    fun searchQuestionIdsByScore(@Param("query") query: String, @Param("limit") limit: Int): List<Long>

    @Query(
        value = """
            SELECT qt2.question_id
            FROM question_tags qt1
            JOIN question_tags qt2 ON qt2.tag_id = qt1.tag_id AND qt2.question_id <> qt1.question_id
            JOIN questions q ON q.id = qt2.question_id
            WHERE qt1.question_id = :questionId AND q.deleted_at IS NULL
            GROUP BY qt2.question_id
            ORDER BY COUNT(*) DESC, qt2.question_id DESC
            LIMIT :limit
        """,
        nativeQuery = true,
    )
    fun findRelatedQuestionIds(@Param("questionId") questionId: Long, @Param("limit") limit: Int): List<Long>
}
