package com.quno.qunobackend.infrastructure.persistence.jpa.repository

import com.quno.qunobackend.infrastructure.persistence.jpa.entity.EngineRunJpaEntity
import com.quno.qunobackend.infrastructure.persistence.jpa.entity.EngineSecretAllowlistJpaEntity
import com.quno.qunobackend.infrastructure.persistence.jpa.entity.EngineSuggestionJpaEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.time.Instant

interface EngineSuggestionJpaRepository : JpaRepository<EngineSuggestionJpaEntity, Long> {
    fun findAllByQuestionIdOrderByIdDesc(questionId: Long): List<EngineSuggestionJpaEntity>

    @Modifying
    @Query(
        "UPDATE EngineSuggestionJpaEntity s SET s.status = 'SUPERSEDED', s.resolvedAt = :at " +
            "WHERE s.questionId = :questionId AND s.status = 'OPEN' AND s.targetVersionNumber < :versionNumber",
    )
    fun supersedeOpenBefore(@Param("questionId") questionId: Long, @Param("versionNumber") versionNumber: Int, @Param("at") at: Instant)
}

interface EngineRunJpaRepository : JpaRepository<EngineRunJpaEntity, Long> {
    fun existsByQuestionIdAndVersionNumberAndEngineAndEngineVersion(questionId: Long, versionNumber: Int, engine: String, engineVersion: Int): Boolean

    @Query(
        value = """
            SELECT q.id FROM questions q
            JOIN question_versions v ON v.id = q.latest_version_id
            WHERE q.deleted_at IS NULL
              AND NOT EXISTS (
                SELECT 1 FROM engine_runs r
                WHERE r.question_id = q.id AND r.version_number = v.version_number
                  AND r.engine = :engine AND r.engine_version = :engineVersion
              )
            ORDER BY q.id
            LIMIT :limit
        """,
        nativeQuery = true,
    )
    fun findQuestionIdsNeedingRun(@Param("engine") engine: String, @Param("engineVersion") engineVersion: Int, @Param("limit") limit: Int): List<Long>
}

interface EngineSecretAllowlistJpaRepository : JpaRepository<EngineSecretAllowlistJpaEntity, Long> {
    fun findAllByQuestionId(questionId: Long): List<EngineSecretAllowlistJpaEntity>
}
