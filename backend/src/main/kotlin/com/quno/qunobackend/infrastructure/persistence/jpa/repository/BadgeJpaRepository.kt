package com.quno.qunobackend.infrastructure.persistence.jpa.repository

import com.quno.qunobackend.infrastructure.persistence.jpa.entity.UserJpaEntity
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.Repository
import org.springframework.data.repository.query.Param

/** Not a CRUD repository — only runs the vote-score-received aggregate query for Badge (Phase 15). */
interface BadgeJpaRepository : Repository<UserJpaEntity, Long> {

    @Query(
        value = """
            SELECT COALESCE(SUM(v.value), 0) FROM votes v
            WHERE (v.target_type = 'QUESTION' AND v.target_id IN (
                SELECT id FROM questions WHERE author_id = :userId AND deleted_at IS NULL
            ))
               OR (v.target_type = 'ANSWER' AND v.target_id IN (
                SELECT id FROM answers WHERE author_id = :userId AND deleted_at IS NULL
            ))
        """,
        nativeQuery = true,
    )
    fun sumVoteScoreReceived(@Param("userId") userId: Long): Long
}
