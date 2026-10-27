package com.quno.qunobackend.infrastructure.persistence.jpa.repository

import com.quno.qunobackend.infrastructure.persistence.jpa.entity.QuestionErrorSignatureJpaEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface QuestionErrorSignatureJpaRepository : JpaRepository<QuestionErrorSignatureJpaEntity, Long> {
    @Modifying
    @Query("DELETE FROM QuestionErrorSignatureJpaEntity s WHERE s.questionId = :questionId")
    fun deleteAllByQuestionId(@Param("questionId") questionId: Long)

    /** Rank 0 = signature hash match, 1 = message key only. Callers pass a non-empty list for both
     * collections (a sentinel when empty) because `IN ()` is not valid SQL. */
    @Query(
        value = """
            SELECT s.question_id
            FROM question_error_signatures s
            WHERE (s.signature_hash IN (:hashes) OR s.message_key IN (:keys))
              AND s.question_id <> :excludeQuestionId
            GROUP BY s.question_id
            ORDER BY MIN(CASE WHEN s.signature_hash IN (:hashes) THEN 0 ELSE 1 END), MAX(s.created_at) DESC
            LIMIT :limit
        """,
        nativeQuery = true,
    )
    fun findMatchingQuestionIds(
        @Param("hashes") hashes: Collection<String>,
        @Param("keys") keys: Collection<String>,
        @Param("excludeQuestionId") excludeQuestionId: Long,
        @Param("limit") limit: Int,
    ): List<Long>
}
