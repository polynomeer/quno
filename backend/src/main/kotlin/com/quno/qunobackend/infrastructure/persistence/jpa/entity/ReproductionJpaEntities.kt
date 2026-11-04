package com.quno.qunobackend.infrastructure.persistence.jpa.entity

import com.quno.qunobackend.domain.reproduction.ReproductionKind
import com.quno.qunobackend.domain.reproduction.ReproductionResult
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.Instant

@Entity
@Table(name = "reproduction_capsules")
class ReproductionCapsuleJpaEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @Column(name = "question_id", nullable = false)
    val questionId: Long,

    @Column(nullable = false)
    val repository: String,

    @Column(nullable = false)
    val ref: String,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    val kind: ReproductionKind,

    @Column(nullable = false)
    val command: String,

    @Column(name = "created_by", nullable = false)
    val createdBy: Long,

    @Column(name = "created_at", nullable = false)
    val createdAt: Instant,

    @Column(name = "superseded_at")
    val supersededAt: Instant?,

    @Column(name = "rerun_requested_at")
    val rerunRequestedAt: Instant?,

    @Column(name = "rerun_requested_by")
    val rerunRequestedBy: Long?,

    @Column(name = "rerun_reason")
    val rerunReason: String?,

    @Column(name = "matrix_package")
    val matrixPackage: String?,

    /** Comma-separated; versions never contain commas. */
    @Column(name = "matrix_versions")
    val matrixVersions: String?,

    @Column(name = "rerun_request_count", nullable = false)
    val rerunRequestCount: Int,
)

@Entity
@Table(name = "reproduction_runs")
class ReproductionRunJpaEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @Column(name = "capsule_id", nullable = false)
    val capsuleId: Long,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    val result: ReproductionResult,

    @Column(name = "exit_code")
    val exitCode: Int?,

    @Column(name = "commit_sha", nullable = false)
    val commitSha: String,

    @Column(nullable = false)
    val environment: String,

    @Column(name = "runner_environment", nullable = false)
    val runnerEnvironment: String,

    @Column(name = "trusted_workflow", nullable = false)
    val trustedWorkflow: Boolean,

    @Column(name = "workflow_ref", nullable = false)
    val workflowRef: String,

    @Column(name = "run_id", nullable = false)
    val runId: Long,

    @Column(name = "run_attempt", nullable = false)
    val runAttempt: Int,

    @Column(name = "run_url", nullable = false)
    val runUrl: String,

    @Column(name = "duration_ms")
    val durationMs: Long?,

    @Column(name = "log_tail")
    val logTail: String?,

    @Column(name = "masked_secret_count", nullable = false)
    val maskedSecretCount: Int,

    @Column(name = "finished_at", nullable = false)
    val finishedAt: Instant,

    @Column(name = "dependency_version")
    val dependencyVersion: String?,

    @Column(name = "event_name")
    val eventName: String?,
)
