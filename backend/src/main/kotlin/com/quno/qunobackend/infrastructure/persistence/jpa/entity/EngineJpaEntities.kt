package com.quno.qunobackend.infrastructure.persistence.jpa.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.Instant

@Entity
@Table(name = "engine_suggestions")
class EngineSuggestionJpaEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,
    @Column(name = "question_id", nullable = false) val questionId: Long,
    @Column(name = "target_version_number", nullable = false) val targetVersionNumber: Int,
    @Column(name = "kind", nullable = false) val kind: String,
    @Column(name = "status", nullable = false) val status: String,
    @Column(name = "engine", nullable = false) val engine: String,
    @Column(name = "engine_version", nullable = false) val engineVersion: Int,
    @Column(name = "input_hash", nullable = false) val inputHash: String,
    @Column(name = "summary", nullable = false) val summary: String,
    @Column(name = "evidence", nullable = false, columnDefinition = "TEXT") val evidence: String,
    @Column(name = "proposed_title") val proposedTitle: String?,
    @Column(name = "proposed_body", columnDefinition = "TEXT") val proposedBody: String?,
    @Column(name = "proposed_environment", columnDefinition = "TEXT") val proposedEnvironment: String?,
    @Column(name = "proposed_logs", columnDefinition = "TEXT") val proposedLogs: String?,
    @Column(name = "env_digest") val envDigest: String?,
    @Column(name = "log_ref") val logRef: String?,
    @Column(name = "created_at", nullable = false) val createdAt: Instant,
    @Column(name = "resolved_at") val resolvedAt: Instant?,
    @Column(name = "resolved_by") val resolvedBy: Long?,
)

@Entity
@Table(name = "engine_runs")
class EngineRunJpaEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,
    @Column(name = "question_id", nullable = false) val questionId: Long,
    @Column(name = "version_number", nullable = false) val versionNumber: Int,
    @Column(name = "engine", nullable = false) val engine: String,
    @Column(name = "engine_version", nullable = false) val engineVersion: Int,
    @Column(name = "input_hash", nullable = false) val inputHash: String,
    @Column(name = "finding_count", nullable = false) val findingCount: Int,
    @Column(name = "ran_at", nullable = false) val ranAt: Instant,
)

@Entity
@Table(name = "engine_secret_allowlist")
class EngineSecretAllowlistJpaEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,
    @Column(name = "question_id", nullable = false) val questionId: Long,
    @Column(name = "value_hash", nullable = false) val valueHash: String,
    @Column(name = "created_at", nullable = false) val createdAt: Instant,
)
