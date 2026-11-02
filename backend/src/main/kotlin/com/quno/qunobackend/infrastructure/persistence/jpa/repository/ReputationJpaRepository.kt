package com.quno.qunobackend.infrastructure.persistence.jpa.repository

import com.quno.qunobackend.infrastructure.persistence.jpa.entity.UserJpaEntity
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.Repository
import org.springframework.data.repository.query.Param

/**
 * Not a CRUD repository — only runs the per-user reputation aggregate query (PLAN.md 9.1).
 * Accepting your own answer doesn't count as "accepted" (ADR-0086): otherwise every promoted
 * card (Phase 79.3) — or any self-answer — would mint +15.
 */
interface ReputationJpaRepository : Repository<UserJpaEntity, Long> {

    @Query(
        value = """
            SELECT
                (SELECT COUNT(*) FROM questions WHERE author_id = :userId AND deleted_at IS NULL) AS questionCount,
                (SELECT COUNT(*) FROM answers WHERE author_id = :userId AND deleted_at IS NULL) AS answerCount,
                (SELECT COUNT(*) FROM answers a
                   JOIN questions q ON q.id = a.question_id
                  WHERE a.author_id = :userId AND a.is_accepted = true AND a.deleted_at IS NULL
                    AND q.author_id <> a.author_id) AS acceptedAnswerCount,
                (SELECT COUNT(*) FROM question_clusters qc
                   JOIN answers a ON a.id = qc.representative_answer_id
                  WHERE a.author_id = :userId) AS superAnswerCount
        """,
        nativeQuery = true,
    )
    fun computeFor(@Param("userId") userId: Long): UserReputationProjection
}
