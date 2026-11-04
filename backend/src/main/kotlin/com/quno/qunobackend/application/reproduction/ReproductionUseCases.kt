package com.quno.qunobackend.application.reproduction

import com.quno.qunobackend.application.verification.QuestionVerificationService
import com.quno.qunobackend.domain.engine.SecretDetector
import com.quno.qunobackend.domain.errorsignature.ErrorSignatureExtractor
import com.quno.qunobackend.domain.question.QuestionAccessDeniedException
import com.quno.qunobackend.domain.question.QuestionNotFoundException
import com.quno.qunobackend.domain.question.QuestionRepository
import com.quno.qunobackend.domain.question.QuestionViewCountRepository
import com.quno.qunobackend.domain.reproduction.DuplicateReproductionRunException
import com.quno.qunobackend.domain.reproduction.ReproductionCapsule
import com.quno.qunobackend.domain.reproduction.ReproductionCapsuleNotFoundException
import com.quno.qunobackend.domain.reproduction.ReproductionCapsuleRepository
import com.quno.qunobackend.domain.reproduction.ReproductionKind
import com.quno.qunobackend.domain.reproduction.ReproductionMatrix
import com.quno.qunobackend.domain.reproduction.ReproductionMetrics
import com.quno.qunobackend.domain.reproduction.ReproductionPlan
import com.quno.qunobackend.domain.reproduction.ReproductionResult
import com.quno.qunobackend.domain.reproduction.ReproductionRun
import com.quno.qunobackend.domain.reproduction.ReproductionRunMismatchException
import com.quno.qunobackend.domain.reproduction.ReproductionRunNotFoundException
import com.quno.qunobackend.domain.reproduction.ReproductionRunRepository
import com.quno.qunobackend.domain.reproduction.ReproductionSchedule
import com.quno.qunobackend.domain.reproduction.RunAttestationVerifier
import com.quno.qunobackend.domain.snapshot.EnvironmentCoreReader
import com.quno.qunobackend.domain.snapshot.EnvironmentRecipe
import com.quno.qunobackend.domain.snapshot.EnvironmentSnapshotRepository
import com.quno.qunobackend.domain.stack.AnswerApplicabilityRepository
import com.quno.qunobackend.domain.stack.QuestionDependencyRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Duration
import java.time.Instant

data class ReproductionCapsuleResult(
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

data class ReproductionRunSummary(
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

/**
 * What the question page shows (Phase 80.3, 81.3): the capsule, the newest run per matrix cell
 * (environment × version), the versions the next run will walk, and the recent history.
 */
data class ReproductionPanelResult(
    val capsule: ReproductionCapsuleResult?,
    val latestByCell: List<ReproductionRunSummary>,
    val plannedVersions: List<String?>,
    val nextDueAt: Instant?,
    val recentRuns: List<ReproductionRunSummary>,
    /** Workflow settings nearest the question's snapshot (Phase 93.6); null without one. */
    val recipe: EnvironmentRecipe? = null,
)

data class ReproductionRunLogResult(val runId: Long, val logTail: String?, val maskedSecretCount: Int)

data class AttachReproductionCapsuleCommand(
    val questionId: Long,
    val actorId: Long,
    val repository: String,
    val ref: String,
    val kind: ReproductionKind,
    val command: String,
    val matrixPackage: String? = null,
    val matrixVersions: List<String> = emptyList(),
)

/** What the workflow says it ran and saw. Where, which commit and which run come from the token instead. */
data class ReportReproductionRunCommand(
    val capsuleId: Long,
    val kind: ReproductionKind,
    val command: String,
    val buildFailed: Boolean,
    val exitCode: Int?,
    val environment: String,
    val durationMs: Long?,
    val logTail: String?,
    /** The matrix cell's version; null outside a matrix. */
    val dependencyVersion: String? = null,
)

data class ReportedRunResult(val runId: Long, val result: ReproductionResult)

private fun ReproductionCapsule.toResult() = ReproductionCapsuleResult(requireNotNull(id), repository, ref, kind, command, createdBy, createdAt, rerunRequestedAt, rerunReason, matrixPackage, matrixVersions)

private fun ReproductionRun.toSummary() = ReproductionRunSummary(requireNotNull(id), result, exitCode, commitSha, environment, runnerEnvironment, trustedWorkflow, runUrl, durationMs, finishedAt, logTail != null, dependencyVersion)

/** The plan for a capsule right now (Phase 81.2): due or not, and which versions. Shared by the page and the workflow. */
@Service
class ReproductionPlanner(
    private val runRepository: ReproductionRunRepository,
    private val viewCountRepository: QuestionViewCountRepository,
    private val dependencyRepository: QuestionDependencyRepository,
    private val applicabilityRepository: AnswerApplicabilityRepository,
) {
    fun plan(capsule: ReproductionCapsule, eventName: String?, now: Instant = Instant.now()): ReproductionPlan {
        val lastRunAt = runRepository.lastFinishedAt(requireNotNull(capsule.id))
        val viewCount = viewCountRepository.countByQuestionId(capsule.questionId)
        val reason = ReproductionSchedule.dueReason(eventName, capsule.rerunRequestedAt != null, lastRunAt, viewCount, now)
        val nextDueAt = lastRunAt?.plus(ReproductionSchedule.intervalFor(viewCount))
        return ReproductionPlan(due = reason != null, reason = reason ?: "not-due", versions = versions(capsule), nextDueAt = nextDueAt)
    }

    fun versions(capsule: ReproductionCapsule): List<String?> {
        val target = capsule.matrixPackage ?: return listOf(null)
        val questionVersion = dependencyRepository.findByQuestionId(capsule.questionId).firstOrNull { it.name.equals(target, ignoreCase = true) }?.version
        val edges = applicabilityRepository.findByQuestionIds(listOf(capsule.questionId))[capsule.questionId].orEmpty()
            .filter { it.name.equals(target, ignoreCase = true) }
            .flatMap { listOfNotNull(it.fromVersion, it.untilVersion) }
        return ReproductionMatrix.plan(capsule, questionVersion, edges)
    }
}

/**
 * Attaching a capsule (Phase 80, ADR-0087). The question's author owns its reproduction target;
 * replacing it supersedes the old capsule so its runs stop counting.
 */
@Service
class AttachReproductionCapsuleUseCase(
    private val questionRepository: QuestionRepository,
    private val capsuleRepository: ReproductionCapsuleRepository,
    private val verificationService: QuestionVerificationService? = null,
) {
    @Transactional
    fun execute(command: AttachReproductionCapsuleCommand): ReproductionCapsuleResult {
        val question = questionRepository.findByIdForUpdate(command.questionId) ?: throw QuestionNotFoundException(command.questionId)
        if (question.authorId != command.actorId) throw QuestionAccessDeniedException(command.questionId)
        val capsule = ReproductionCapsule.attach(command.questionId, command.repository, command.ref, command.kind, command.command, command.actorId, command.matrixPackage, command.matrixVersions)
        capsuleRepository.findActiveByQuestionId(command.questionId)?.let { capsuleRepository.save(it.supersede()) }
        val saved = capsuleRepository.save(capsule).toResult()
        // The old capsule's runs stop counting, so a level earned through them goes with them.
        verificationService?.refresh(command.questionId)
        return saved
    }
}

@Service
class GetReproductionPanelUseCase(
    private val capsuleRepository: ReproductionCapsuleRepository,
    private val runRepository: ReproductionRunRepository,
    private val planner: ReproductionPlanner,
    private val snapshotRepository: EnvironmentSnapshotRepository,
    private val coreReader: EnvironmentCoreReader,
) {
    fun execute(questionId: Long): ReproductionPanelResult {
        val recipe = snapshotRepository.findByQuestionId(questionId)?.let { EnvironmentRecipe.from(coreReader.read(it.document)) }
        val capsule = capsuleRepository.findActiveByQuestionId(questionId) ?: return ReproductionPanelResult(null, emptyList(), emptyList(), null, emptyList(), recipe)
        val capsuleId = requireNotNull(capsule.id)
        val plan = planner.plan(capsule, eventName = null)
        return ReproductionPanelResult(
            capsule = capsule.toResult(),
            latestByCell = runRepository.findLatestPerCell(capsuleId).map { it.toSummary() },
            plannedVersions = plan.versions,
            nextDueAt = if (plan.due) null else plan.nextDueAt,
            recentRuns = runRepository.findRecentByCapsuleId(capsuleId, RECENT_RUNS).map { it.toSummary() },
            recipe = recipe,
        )
    }

    fun log(questionId: Long, runId: Long): ReproductionRunLogResult {
        val run = runRepository.findById(runId) ?: throw ReproductionRunNotFoundException(runId)
        val capsule = capsuleRepository.findById(run.capsuleId)
        // The URL names the question, so a run of another question's capsule is "not found" here.
        if (capsule == null || capsule.questionId != questionId) throw ReproductionRunNotFoundException(runId)
        return ReproductionRunLogResult(runId, run.logTail, run.maskedSecretCount)
    }

    companion object {
        const val RECENT_RUNS = 10
    }
}

/** "이의 제기 → 재실행" (Phase 80.3): anyone signed in who can see the question. Cleared by the next run. */
@Service
class RequestReproductionRerunUseCase(
    private val capsuleRepository: ReproductionCapsuleRepository,
) {
    @Transactional
    fun execute(questionId: Long, actorId: Long, reason: String): ReproductionCapsuleResult {
        val active = capsuleRepository.findActiveByQuestionId(questionId) ?: throw ReproductionCapsuleNotFoundException("question $questionId has no reproduction capsule")
        val capsule = capsuleRepository.findByIdForUpdate(requireNotNull(active.id)) ?: throw ReproductionCapsuleNotFoundException("question $questionId has no reproduction capsule")
        return capsuleRepository.save(capsule.requestRerun(actorId, reason)).toResult()
    }
}

/**
 * Records a run reported by GitHub Actions (Phase 80.2, ADR-0087). The OIDC token is the only
 * credential: it must come from the capsule's repository (and its pinned commit, if any), and the
 * reported spec must be the capsule's current one. The log tail is secret-masked before it is
 * stored and only the newest [KEEP_LOGS] runs keep theirs.
 */
@Service
class RecordReproductionRunUseCase(
    private val verifier: RunAttestationVerifier,
    private val capsuleRepository: ReproductionCapsuleRepository,
    private val runRepository: ReproductionRunRepository,
    private val verificationService: QuestionVerificationService? = null,
) {
    @Transactional
    fun execute(attestationToken: String, command: ReportReproductionRunCommand): ReportedRunResult {
        val attestation = verifier.verify(attestationToken)
        val capsule = capsuleRepository.findByIdForUpdate(command.capsuleId)
            ?: throw ReproductionCapsuleNotFoundException("reproduction capsule ${command.capsuleId} not found")
        val capsuleId = requireNotNull(capsule.id)
        capsule.checkRunFrom(attestation)
        if (!capsule.matches(command.kind, command.command)) throw ReproductionRunMismatchException("the run's kind/command differ from capsule $capsuleId")
        val dependencyVersion = command.dependencyVersion?.trim()?.takeIf { it.isNotEmpty() }?.take(MAX_VERSION)
        capsule.checkCell(dependencyVersion)
        if (runRepository.exists(capsuleId, attestation.runId, attestation.runAttempt)) throw DuplicateReproductionRunException(attestation.runId, attestation.runAttempt)

        val tail = command.logTail?.let(::tailOf)
        // From the raw tail: masking a value must not change which error this was.
        val signatures = ErrorSignatureExtractor.extractAll(tail).take(MAX_SIGNATURES)
        val masked = tail?.let { SecretDetector.find(it).count { finding -> it.substring(finding.start, finding.end) != SecretDetector.REDACTED } } ?: 0
        val run = runRepository.save(
            ReproductionRun(
                id = null,
                capsuleId = capsuleId,
                result = ReproductionResult.of(command.buildFailed, command.exitCode),
                exitCode = command.exitCode,
                commitSha = attestation.sha,
                environment = command.environment.trim().take(MAX_ENVIRONMENT).ifEmpty { "unknown" },
                runnerEnvironment = attestation.runnerEnvironment.take(20),
                trustedWorkflow = attestation.trustedWorkflow,
                workflowRef = attestation.workflowRef.take(500),
                runId = attestation.runId,
                runAttempt = attestation.runAttempt,
                runUrl = "https://github.com/${attestation.repository}/actions/runs/${attestation.runId}/attempts/${attestation.runAttempt}",
                durationMs = command.durationMs?.takeIf { it >= 0 },
                logTail = tail?.let { SecretDetector.redact(it) },
                maskedSecretCount = masked,
                finishedAt = Instant.now(),
                dependencyVersion = dependencyVersion,
                eventName = attestation.eventName,
                signatureHashes = signatures.mapNotNull { it.signatureHash }.distinct(),
                messageKeys = signatures.map { it.messageKey }.distinct(),
                exceptionClass = signatures.firstOrNull()?.exceptionClass?.take(255),
            ),
        )
        if (capsule.rerunRequestedAt != null) capsuleRepository.save(capsule.rerunAnswered())
        runRepository.pruneLogs(capsuleId, KEEP_LOGS)
        verificationService?.refresh(capsule.questionId)
        return ReportedRunResult(requireNotNull(run.id), run.result)
    }

    /** The last [MAX_LOG_LINES] lines, then at most [MAX_LOG_CHARS] characters from the end. */
    private fun tailOf(log: String): String = log.lines().takeLast(MAX_LOG_LINES).joinToString("\n").takeLast(MAX_LOG_CHARS)

    companion object {
        const val KEEP_LOGS = 20
        const val MAX_LOG_LINES = 200
        const val MAX_LOG_CHARS = 64 * 1024
        const val MAX_ENVIRONMENT = 200
        const val MAX_VERSION = 100
        const val MAX_SIGNATURES = 10
    }
}

/**
 * What a scheduled workflow asks before it runs (Phase 81.2, ADR-0088): the same token as a report
 * proves the caller, and the answer is the capsule's matrix if it is due, or "skip" with when it
 * will be. Nothing about the question is returned — only the capsule's own package versions.
 */
@Service
class PlanReproductionRunUseCase(
    private val verifier: RunAttestationVerifier,
    private val capsuleRepository: ReproductionCapsuleRepository,
    private val planner: ReproductionPlanner,
) {
    fun execute(attestationToken: String, capsuleId: Long): ReproductionPlan {
        val attestation = verifier.verify(attestationToken)
        val capsule = capsuleRepository.findById(capsuleId) ?: throw ReproductionCapsuleNotFoundException("reproduction capsule $capsuleId not found")
        capsule.checkRunFrom(attestation)
        val plan = planner.plan(capsule, attestation.eventName)
        return if (plan.due) plan else plan.copy(versions = emptyList())
    }
}

/** Runner health over the last [WINDOW] (Phase 81.4). */
@Service
class GetReproductionMetricsUseCase(
    private val runRepository: ReproductionRunRepository,
) {
    fun execute(now: Instant = Instant.now()): ReproductionMetrics = runRepository.metrics(now.minus(WINDOW))

    companion object {
        val WINDOW: Duration = Duration.ofDays(30)
    }
}
