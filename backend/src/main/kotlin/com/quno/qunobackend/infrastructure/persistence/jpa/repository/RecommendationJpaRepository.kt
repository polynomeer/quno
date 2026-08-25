package com.quno.qunobackend.infrastructure.persistence.jpa.repository

import com.quno.qunobackend.infrastructure.persistence.jpa.entity.QuestionJpaEntity
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.Repository
import org.springframework.data.repository.query.Param

/** Not a CRUD repository — only runs the tag-follow recommendation ranking query. */
interface RecommendationJpaRepository : Repository<QuestionJpaEntity, Long> {

    @Query(
        value = """
            SELECT m.id
            FROM (
                SELECT q.id, q.created_at, COUNT(DISTINCT qt.tag_id) AS matched_tag_count
                FROM user_tag_follows utf
                JOIN question_tags qt ON qt.tag_id = utf.tag_id
                JOIN questions q ON q.id = qt.question_id
                WHERE utf.user_id = :userId
                  AND q.deleted_at IS NULL
                  AND q.author_id <> :userId
                GROUP BY q.id, q.created_at
            ) m
            LEFT JOIN (
                SELECT question_id, COUNT(*) AS answer_count
                FROM answers
                WHERE deleted_at IS NULL
                GROUP BY question_id
            ) a ON a.question_id = m.id
            ORDER BY (m.matched_tag_count * 3 + LEAST(COALESCE(a.answer_count, 0), 5)) DESC, m.created_at DESC
            LIMIT :limit
        """,
        nativeQuery = true,
    )
    fun recommendQuestionIdsByTagFollows(@Param("userId") userId: Long, @Param("limit") limit: Int): List<Long>
}
