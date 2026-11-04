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
import java.time.Instant

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
            SELECT DISTINCT ON (environment, dependency_version) * FROM reproduction_runs
             WHERE capsule_id = :capsuleId
             ORDER BY environment, dependency_version, finished_at DESC, id DESC
        """,
        nativeQuery = true,
    )
    fun findLatestPerCell(@Param("capsuleId") capsuleId: Long): List<ReproductionRunJpaEntity>

    @Query("select max(r.finishedAt) from ReproductionRunJpaEntity r where r.capsuleId = :capsuleId")
    fun lastFinishedAt(@Param("capsuleId") capsuleId: Long): Instant?

    @Query(
        value = """
            SELECT
                (SELECT COUNT(*) FROM reproduction_runs WHERE finished_at >= :since) AS runs,
                (SELECT COUNT(*) FROM reproduction_runs WHERE finished_at >= :since AND result IN ('REPRODUCED', 'NOT_REPRODUCED')) AS conclusiveRuns,
                (SELECT COUNT(*) FROM reproduction_runs WHERE finished_at >= :since AND trusted_workflow) AS trustedRuns,
                (SELECT COALESCE(SUM(duration_ms), 0) / 60000 FROM reproduction_runs WHERE finished_at >= :since) AS runnerMinutes,
                (SELECT COUNT(*) FROM (
                    SELECT 1 FROM reproduction_runs WHERE finished_at >= :since
                     GROUP BY capsule_id, environment, dependency_version) cells) AS cells,
                (SELECT COUNT(*) FROM (
                    SELECT 1 FROM reproduction_runs WHERE finished_at >= :since
                     GROUP BY capsule_id, environment, dependency_version, commit_sha
                    HAVING COUNT(*) FILTER (WHERE result = 'REPRODUCED') > 0
                       AND COUNT(*) FILTER (WHERE result = 'NOT_REPRODUCED') > 0) flaky) AS flakyCells,
                (SELECT COUNT(*) FROM reproduction_capsules WHERE superseded_at IS NULL) AS activeCapsules,
                (SELECT COALESCE(SUM(rerun_request_count), 0) FROM reproduction_capsules) AS disputes
        """,
        nativeQuery = true,
    )
    fun metrics(@Param("since") since: Instant): ReproductionMetricsProjection

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

interface ReproductionMetricsProjection {
    fun getRuns(): Long
    fun getConclusiveRuns(): Long
    fun getTrustedRuns(): Long
    fun getRunnerMinutes(): Long
    fun getCells(): Long
    fun getFlakyCells(): Long
    fun getActiveCapsules(): Long
    fun getDisputes(): Long
}
