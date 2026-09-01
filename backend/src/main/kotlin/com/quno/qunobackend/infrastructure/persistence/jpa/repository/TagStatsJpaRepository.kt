package com.quno.qunobackend.infrastructure.persistence.jpa.repository

import com.quno.qunobackend.infrastructure.persistence.jpa.entity.TagJpaEntity
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.Repository
import org.springframework.data.repository.query.Param

interface TagContributorProjection {
    fun getUserId(): Long
    fun getNickname(): String
    fun getAnswerCount(): Long
}

/** Not a CRUD repository — only runs Tag Detail's aggregate queries (Phase 28, ADR-0040), same
 * shape as SearchJpaRepository/DashboardJpaRepository. */
interface TagStatsJpaRepository : Repository<TagJpaEntity, Long> {

    @Query(
        value = """
            SELECT q.id
            FROM questions q
            JOIN question_tags qt ON qt.question_id = q.id
            WHERE qt.tag_id = :tagId AND q.deleted_at IS NULL
            ORDER BY q.created_at DESC
            LIMIT :limit
        """,
        nativeQuery = true,
    )
    fun findLatestQuestionIds(@Param("tagId") tagId: Long, @Param("limit") limit: Int): List<Long>

    @Query(
        value = """
            SELECT q.id
            FROM questions q
            JOIN question_tags qt ON qt.question_id = q.id
            LEFT JOIN answers a ON a.question_id = q.id AND a.deleted_at IS NULL
            WHERE qt.tag_id = :tagId AND q.deleted_at IS NULL
            GROUP BY q.id
            HAVING COUNT(a.id) = 0
            ORDER BY q.created_at DESC
            LIMIT :limit
        """,
        nativeQuery = true,
    )
    fun findUnansweredQuestionIds(@Param("tagId") tagId: Long, @Param("limit") limit: Int): List<Long>

    @Query(
        value = """
            SELECT q.id
            FROM questions q
            JOIN question_tags qt ON qt.question_id = q.id
            LEFT JOIN (
                SELECT target_id, SUM(value) AS vote_score FROM votes WHERE target_type = 'QUESTION' GROUP BY target_id
            ) v ON v.target_id = q.id
            WHERE qt.tag_id = :tagId AND q.deleted_at IS NULL
            ORDER BY COALESCE(v.vote_score, 0) DESC, q.created_at DESC
            LIMIT :limit
        """,
        nativeQuery = true,
    )
    fun findTopQuestionIds(@Param("tagId") tagId: Long, @Param("limit") limit: Int): List<Long>

    @Query(
        value = """
            SELECT a.author_id AS userId, u.nickname AS nickname, COUNT(*) AS answerCount
            FROM answers a
            JOIN question_tags qt ON qt.question_id = a.question_id
            JOIN users u ON u.id = a.author_id
            WHERE qt.tag_id = :tagId AND a.deleted_at IS NULL
            GROUP BY a.author_id, u.nickname
            ORDER BY answerCount DESC, a.author_id ASC
            LIMIT :limit
        """,
        nativeQuery = true,
    )
    fun findTopContributors(@Param("tagId") tagId: Long, @Param("limit") limit: Int): List<TagContributorProjection>

    @Query(
        value = """
            SELECT qt2.tag_id
            FROM question_tags qt1
            JOIN question_tags qt2 ON qt2.question_id = qt1.question_id AND qt2.tag_id <> qt1.tag_id
            JOIN tags t ON t.id = qt2.tag_id AND t.deleted_at IS NULL
            WHERE qt1.tag_id = :tagId
            GROUP BY qt2.tag_id
            ORDER BY COUNT(*) DESC, qt2.tag_id ASC
            LIMIT :limit
        """,
        nativeQuery = true,
    )
    fun findRelatedTagIds(@Param("tagId") tagId: Long, @Param("limit") limit: Int): List<Long>
}
