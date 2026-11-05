package com.quno.qunobackend.interfaces.api.reproduction

import com.quno.qunobackend.application.reproduction.AttachReproductionCapsuleCommand
import com.quno.qunobackend.application.reproduction.AttachReproductionCapsuleUseCase
import com.quno.qunobackend.application.reproduction.GetReproductionMetricsUseCase
import com.quno.qunobackend.application.reproduction.GetReproductionPanelUseCase
import com.quno.qunobackend.application.reproduction.PlanReproductionRunUseCase
import com.quno.qunobackend.application.reproduction.RecordReproductionRunUseCase
import com.quno.qunobackend.application.reproduction.ReportReproductionRunCommand
import com.quno.qunobackend.application.reproduction.ReproductionCapsuleResult
import com.quno.qunobackend.application.reproduction.ReproductionPanelResult
import com.quno.qunobackend.application.reproduction.ReproductionRunComparison
import com.quno.qunobackend.application.reproduction.ReproductionRunLogResult
import com.quno.qunobackend.application.reproduction.ReproductionRunSummary
import com.quno.qunobackend.application.reproduction.ReproductionTransitionResult
import com.quno.qunobackend.application.reproduction.RequestReproductionRerunUseCase
import com.quno.qunobackend.domain.reproduction.InvalidRunAttestationException
import com.quno.qunobackend.domain.reproduction.ReproductionKind
import com.quno.qunobackend.domain.reproduction.ReproductionMetrics
import com.quno.qunobackend.domain.reproduction.ReproductionResult
import com.quno.qunobackend.domain.snapshot.EnvironmentRecipe
import jakarta.validation.Valid
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Size
import org.springframework.http.HttpStatus
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import java.time.Instant

/**
 * Reproduction runner v0 (Phase 80, ADR-0087). Everything under `/questions/{id}/reproduction`
 * goes through the question visibility gate (ADR-0085); `/reproduction-runs` is called by GitHub
 * Actions and authenticated by the OIDC token in [ATTESTATION_HEADER] alone.
 */
@RestController
@RequestMapping("/api/v1")
class ReproductionController(
    private val attachUseCase: AttachReproductionCapsuleUseCase,
    private val panelUseCase: GetReproductionPanelUseCase,
    private val rerunUseCase: RequestReproductionRerunUseCase,
    private val recordUseCase: RecordReproductionRunUseCase,
    private val planUseCase: PlanReproductionRunUseCase,
    private val metricsUseCase: GetReproductionMetricsUseCase,
) {

    @GetMapping("/questions/{id}/reproduction")
    fun panel(@PathVariable id: Long): ReproductionPanelResponse = panelUseCase.execute(id).toResponse()

    @PutMapping("/questions/{id}/reproduction")
    fun attach(
        @AuthenticationPrincipal actorId: Long,
        @PathVariable id: Long,
        @Valid @RequestBody request: AttachReproductionCapsuleRequest,
    ): ReproductionCapsuleResponse = attachUseCase.execute(
        AttachReproductionCapsuleCommand(id, actorId, request.repository, request.ref, requireNotNull(request.kind), request.command, request.matrixPackage, request.matrixVersions),
    ).toResponse()

    @PostMapping("/questions/{id}/reproduction/rerun-requests")
    fun requestRerun(
        @AuthenticationPrincipal actorId: Long,
        @PathVariable id: Long,
        @Valid @RequestBody request: RerunRequest,
    ): ReproductionCapsuleResponse = rerunUseCase.execute(id, actorId, request.reason).toResponse()

    @GetMapping("/questions/{id}/reproduction/runs/{runId}/log")
    fun log(@PathVariable id: Long, @PathVariable runId: Long): ReproductionRunLogResponse = panelUseCase.log(id, runId).toResponse()

    /** The version before against the release, line by line (Phase 82.3, ADR-0090). */
    @GetMapping("/questions/{id}/reproduction/runs/{runId}/compare")
    fun compare(@PathVariable id: Long, @PathVariable runId: Long): ReproductionRunComparison = panelUseCase.compare(id, runId)

    @PostMapping("/reproduction-runs")
    @ResponseStatus(HttpStatus.CREATED)
    fun report(
        @RequestHeader(ATTESTATION_HEADER, required = false) attestation: String?,
        @Valid @RequestBody request: ReportReproductionRunRequest,
    ): ReportedRunResponse {
        val token = attestation?.trim()?.takeIf { it.isNotEmpty() } ?: throw InvalidRunAttestationException("missing $ATTESTATION_HEADER")
        val result = recordUseCase.execute(
            token,
            ReportReproductionRunCommand(
                capsuleId = requireNotNull(request.capsuleId),
                kind = requireNotNull(request.kind),
                command = request.command,
                buildFailed = request.buildFailed,
                exitCode = request.exitCode,
                environment = request.environment,
                durationMs = request.durationMs,
                logTail = request.logTail,
                dependencyVersion = request.dependencyVersion,
            ),
        )
        return ReportedRunResponse(result.runId, result.result)
    }

    /** Asked by the scheduled workflow before it runs (Phase 81.2): due or not, and the matrix. */
    @PostMapping("/reproduction-runs/plan")
    fun plan(
        @RequestHeader(ATTESTATION_HEADER, required = false) attestation: String?,
        @Valid @RequestBody request: PlanRequest,
    ): ReproductionPlanResponse {
        val token = attestation?.trim()?.takeIf { it.isNotEmpty() } ?: throw InvalidRunAttestationException("missing $ATTESTATION_HEADER")
        val plan = planUseCase.execute(token, requireNotNull(request.capsuleId))
        return ReproductionPlanResponse(plan.due, plan.reason, plan.versions.map { MatrixCellResponse(it) }, plan.nextDueAt)
    }

    /** Runner health over the last 30 days (Phase 81.4); signed-in users, like `/metrics`. */
    @GetMapping("/metrics/reproduction")
    fun metrics(): ReproductionMetrics = metricsUseCase.execute()

    companion object {
        const val ATTESTATION_HEADER = "X-Quno-Attestation"
    }
}

data class AttachReproductionCapsuleRequest(
    @field:NotBlank @field:Size(max = 200)
    val repository: String,
    @field:NotBlank @field:Size(max = 255)
    val ref: String,
    @field:NotNull
    val kind: ReproductionKind?,
    @field:NotBlank @field:Size(max = 1000)
    val command: String,
    /** Walk several versions of this package (Phase 81, ADR-0088). */
    @field:Size(max = 200)
    val matrixPackage: String? = null,
    @field:Size(max = 5)
    val matrixVersions: List<String> = emptyList(),
)

data class PlanRequest(
    @field:NotNull
    val capsuleId: Long?,
)

/** `version` is null for a capsule without a matrix package; the workflow then runs one cell. */
data class MatrixCellResponse(val version: String?)

data class ReproductionPlanResponse(val due: Boolean, val reason: String, val matrix: List<MatrixCellResponse>, val nextDueAt: Instant?)

data class RerunRequest(
    @field:NotBlank @field:Size(max = 500)
    val reason: String,
)

data class ReportReproductionRunRequest(
    @field:NotNull
    val capsuleId: Long?,
    @field:NotNull
    val kind: ReproductionKind?,
    @field:NotBlank @field:Size(max = 1000)
    val command: String,
    val buildFailed: Boolean = false,
    val exitCode: Int? = null,
    @field:Size(max = 200)
    val environment: String = "",
    val durationMs: Long? = null,
    /** Trimmed to the last 200 lines / 64KB on the server anyway; a hard cap keeps the request small. */
    @field:Size(max = 256 * 1024)
    val logTail: String? = null,
    @field:Size(max = 100)
    val dependencyVersion: String? = null,
)

data class ReportedRunResponse(val runId: Long, val result: ReproductionResult)

data class ReproductionCapsuleResponse(
    val id: Long,
    val repository: String,
    val ref: String,
    val kind: ReproductionKind,
    val command: String,
    val createdBy: Long,
    val createdAt: Instant,
    val rerunRequestedAt: Instant?,
    val rerunReason: String?,
    val matrixPackage: String?,
    val matrixVersions: List<String>,
)

data class ReproductionRunResponse(
    val id: Long,
    val result: ReproductionResult,
    val exitCode: Int?,
    val commitSha: String,
    val environment: String,
    val runnerEnvironment: String,
    val trustedWorkflow: Boolean,
    val runUrl: String,
    val durationMs: Long?,
    val finishedAt: Instant,
    val hasLog: Boolean,
    val dependencyVersion: String?,
)

data class ReproductionPanelResponse(
    val capsule: ReproductionCapsuleResponse?,
    /** Newest run per matrix cell (environment × dependencyVersion). */
    val latestByCell: List<ReproductionRunResponse>,
    /** Versions the next run walks; `[null]` without a matrix package. */
    val plannedVersions: List<String?>,
    /** When the scheduled run is next due; null when it is due now. */
    val nextDueAt: Instant?,
    val recentRuns: List<ReproductionRunResponse>,
    /** Workflow settings nearest the question's snapshot (ADR-0089); null without one. */
    val recipe: EnvironmentRecipe?,
    /** Newest first: releases that changed behaviour (ADR-0090). */
    val transitions: List<ReproductionTransitionResult>,
)

data class ReproductionRunLogResponse(val runId: Long, val logTail: String?, val maskedSecretCount: Int)

private fun ReproductionCapsuleResult.toResponse() = ReproductionCapsuleResponse(id, repository, ref, kind, command, createdBy, createdAt, rerunRequestedAt, rerunReason, matrixPackage, matrixVersions)

private fun ReproductionRunSummary.toResponse() = ReproductionRunResponse(id, result, exitCode, commitSha, environment, runnerEnvironment, trustedWorkflow, runUrl, durationMs, finishedAt, hasLog, dependencyVersion)

private fun ReproductionPanelResult.toResponse() = ReproductionPanelResponse(capsule?.toResponse(), latestByCell.map { it.toResponse() }, plannedVersions, nextDueAt, recentRuns.map { it.toResponse() }, recipe, transitions)

private fun ReproductionRunLogResult.toResponse() = ReproductionRunLogResponse(runId, logTail, maskedSecretCount)
