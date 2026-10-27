package com.quno.qunobackend.infrastructure.persistence.jpa.repository

import com.quno.qunobackend.infrastructure.persistence.jpa.entity.UserStackDependencyJpaEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface UserStackDependencyJpaRepository : JpaRepository<UserStackDependencyJpaEntity, Long> {
    @Modifying
    @Query("DELETE FROM UserStackDependencyJpaEntity d WHERE d.userId = :userId")
    fun deleteAllByUserId(@Param("userId") userId: Long)

    fun findAllByUserIdOrderByIdAsc(userId: Long): List<UserStackDependencyJpaEntity>

    /** Ward suggestions (ADR-0078): packages match on (ecosystem, name), versions are ignored. */
    @Query(
        value = """
            SELECT qd.question_id AS questionId, COUNT(DISTINCT (qd.ecosystem, qd.name)) AS sharedCount
            FROM question_dependencies qd
            JOIN user_stack_dependencies us ON us.ecosystem = qd.ecosystem AND us.name = qd.name AND us.user_id = :userId
            JOIN questions q ON q.id = qd.question_id
            WHERE q.deleted_at IS NULL
              AND q.author_id <> :userId
              AND NOT EXISTS (SELECT 1 FROM watches w WHERE w.user_id = :userId AND w.question_id = q.id)
            GROUP BY qd.question_id, q.created_at
            ORDER BY sharedCount DESC, q.created_at DESC
            LIMIT :limit
        """,
        nativeQuery = true,
    )
    fun findQuestionsSharingStack(@Param("userId") userId: Long, @Param("limit") limit: Int): List<StackMatchRow>
}

interface StackMatchRow {
    fun getQuestionId(): Long
    fun getSharedCount(): Long
}
