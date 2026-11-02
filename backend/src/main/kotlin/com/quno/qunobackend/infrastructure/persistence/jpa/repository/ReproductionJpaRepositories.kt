package com.quno.qunobackend.infrastructure.persistence.jpa.repository

import com.quno.qunobackend.infrastructure.persistence.jpa.entity.ReproductionCapsuleJpaEntity
import com.quno.qunobackend.infrastructure.persistence.jpa.entity.ReproductionRunJpaEntity
import jakarta.persistence.LockModeType
import org.springframework.data.domain.Limit
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Lock
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface ReproductionCapsuleJpaRepository : JpaRepository<ReproductionCapsuleJpaEntity, Long> {
    fun findByQuestionIdAndSupersededAtIsNull(questionId: Long): ReproductionCapsuleJpaEntity?

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from ReproductionCapsuleJpaEntity c where c.id = :id")
    fun findByIdForUpdate(@Param("id") id: Long): ReproductionCapsuleJpaEntity?
}

interface ReproductionRunJpaRepository : JpaRepository<ReproductionRunJpaEntity, Long> {
    fun existsByCapsuleIdAndRunIdAndRunAttempt(capsuleId: Long, runId: Long, runAttempt: Int): Boolean

    fun findByCapsuleIdOrderByFinishedAtDescIdDesc(capsuleId: Long, limit: Limit): List<ReproductionRunJpaEntity>

    @Query(
        value = """
            SELECT DISTINCT ON (environment) * FROM reproduction_runs
             WHERE capsule_id = :capsuleId
             ORDER BY environment, finished_at DESC, id DESC
        """,
        nativeQuery = true,
    )
    fun findLatestPerEnvironment(@Param("capsuleId") capsuleId: Long): List<ReproductionRunJpaEntity>

    @Modifying
    @Query(
        value = """
            UPDATE reproduction_runs SET log_tail = NULL
             WHERE capsule_id = :capsuleId AND log_tail IS NOT NULL
               AND id NOT IN (SELECT id FROM reproduction_runs WHERE capsule_id = :capsuleId ORDER BY finished_at DESC, id DESC LIMIT :keep)
        """,
        nativeQuery = true,
    )
    fun pruneLogs(@Param("capsuleId") capsuleId: Long, @Param("keep") keep: Int)
}
