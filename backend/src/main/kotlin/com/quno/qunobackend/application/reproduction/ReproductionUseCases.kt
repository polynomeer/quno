package com.quno.qunobackend.application.reproduction

import com.quno.qunobackend.application.verification.QuestionVerificationService
import com.quno.qunobackend.domain.answer.AnswerRepository
import com.quno.qunobackend.domain.common.OutboxEvent
import com.quno.qunobackend.domain.common.OutboxEventRepository
import com.quno.qunobackend.domain.common.OutboxEventTypes
import com.quno.qunobackend.domain.engine.SecretDetector
import com.quno.qunobackend.domain.errorsignature.ErrorSignatureExtractor
import com.quno.qunobackend.domain.question.DiffLine
import com.quno.qunobackend.domain.question.QuestionAccessDeniedException
import com.quno.qunobackend.domain.question.QuestionNotFoundException
import com.quno.qunobackend.domain.question.QuestionRepository
import com.quno.qunobackend.domain.question.QuestionViewCountRepository
import com.quno.qunobackend.domain.question.TextDiffer
import com.quno.qunobackend.domain.qunobot.TechnologyReleaseRepository
import com.quno.qunobackend.domain.qunobot.TrackedTechnologies
import com.quno.qunobackend.domain.reproduction.DuplicateReproductionRunException
import com.quno.qunobackend.domain.reproduction.ReleaseRegression
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
import com.quno.qunobackend.domain.reproduction.ReproductionTransition
import com.quno.qunobackend.domain.reproduction.RunAttestationVerifier
import com.quno.qunobackend.domain.snapshot.EnvironmentCoreReader
import com.quno.qunobackend.domain.snapshot.EnvironmentRecipe
import com.quno.qunobackend.domain.snapshot.EnvironmentSnapshotRepository
import com.quno.qunobackend.domain.stack.AnswerApplicabilityRepository
import com.quno.qunobackend.domain.stack.QuestionDependencyRepository
import com.quno.qunobackend.domain.stack.Version
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
    /** Newest first: releases that changed behaviour (Phase 82, ADR-0090). */
    val transitions: List<ReproductionTransitionResult> = emptyList(),
)

data class ReproductionTransitionResult(
    val runId: Long,
    val transition: ReproductionTransition,
    val fromVersion: String?,
    val toVersion: String?,
    val environment: String,
    val runUrl: String,
    val finishedAt: Instant,
)

/** Two runs' logs line by line: the version before against the release (Phase 82.3). */
data class ReproductionRunComparison(
    val transition: ReproductionTransition,
    val fromVersion: String?,
    val toVersion: String?,
    /** False when either log is gone; [lines] is then empty. */
    val logsAvailable: Boolean,
    val lines: List<DiffLine>,
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

private fun ReproductionRun.toTransition() = ReproductionTransitionResult(requireNotNull(id), requireNotNull(transition), comparedVersion, dependencyVersion, environment, runUrl, finishedAt)

private fun ReproductionRun.toSummary() = ReproductionRunSummary(requireNotNull(id), result, exitCode, commitSha, environment, runnerEnvironment, trustedWorkflow, runUrl, durationMs, finishedAt, logTail != null, dependencyVersion)

/** The plan for a capsule right now (Phase 81.2): due or not, and which versions. Shared by the page and the workflow. */
@Service
class ReproductionPlanner(
    private val runRepository: ReproductionRunRepository,
    private val viewCountRepository: QuestionViewCountRepository,
    private val dependencyRepository: QuestionDependencyRepository,
    private val applicabilityRepository: AnswerApplicabilityRepository,
    private val releaseRepository: TechnologyReleaseRepository,
) {
    fun plan(capsule: ReproductionCapsule, eventName: String?, now: Instant = Instant.now()): ReproductionPlan {
        val capsuleId = requireNotNull(capsule.id)
        val lastRunAt = runRepository.lastFinishedAt(capsuleId)
        val viewCount = viewCountRepository.countByQuestionId(capsule.questionId)
        // No fan-out when a release lands: the daily scan updates the table and the next plan sees it (ADR-0090).
        val release = latestRelease(capsule)?.let(Version::parseOrNull)
        val releaseUntested = release != null && runRepository.findLatestPerCell(capsuleId).none { run -> run.dependencyVersion?.let(Version::parseOrNull) == release }
        val reason = ReproductionSchedule.dueReason(eventName, capsule.rerunRequestedAt != null, lastRunAt, viewCount, now, releaseUntested)
        val nextDueAt = lastRunAt?.plus(ReproductionSchedule.intervalFor(viewCount))
        return ReproductionPlan(due = reason != null, reason = reason ?: "not-due", versions = versions(capsule), nextDueAt = nextDueAt)
    }

    fun versions(capsule: ReproductionCapsule): List<String?> {
        val target = capsule.matrixPackage ?: return listOf(null)
        val questionVersion = dependencyRepository.findByQuestionId(capsule.questionId).firstOrNull { it.name.equals(target, ignoreCase = true) }?.version
        val edges = applicabilityRepository.findByQuestionIds(listOf(capsule.questionId))[capsule.questionId].orEmpty()
            .filter { it.name.equals(target, ignoreCase = true) }
            .flatMap { listOfNotNull(it.fromVersion, it.untilVersion) }
        return ReproductionMatrix.plan(capsule, questionVersion, edges, latestRelease(capsule))
    }

    /** The newest release of the tracked technology the capsule's package belongs to, if it is tracked. */
    fun latestRelease(capsule: ReproductionCapsule): String? = capsule.matrixPackage?.let(TrackedTechnologies::tagForPackage)?.let(releaseRepository::findByTagSlug)?.latestVersion
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
            transitions = runRepository.findTransitions(capsuleId, MAX_TRANSITIONS).map { it.toTransition() },
        )
    }

    fun compare(questionId: Long, runId: Long): ReproductionRunComparison {
        val run = owned(questionId, runId)
        val transition = run.transition ?: throw ReproductionRunNotFoundException(runId)
        val compared = run.comparedRunId?.let(runRepository::findById)
        val before = compared?.logTail
        val after = run.logTail
        return ReproductionRunComparison(
            transition = transition,
            fromVersion = run.comparedVersion,
            toVersion = run.dependencyVersion,
            logsAvailable = before != null && after != null,
            lines = if (before != null && after != null) TextDiffer.diffLines(before, after) else emptyList(),
        )
    }

    fun log(questionId: Long, runId: Long): ReproductionRunLogResult {
        val run = owned(questionId, runId)
        return ReproductionRunLogResult(runId, run.logTail, run.maskedSecretCount)
    }

    /** The URL names the question, so a run of another question's capsule is "not found" here. */
    private fun owned(questionId: Long, runId: Long): ReproductionRun {
        val run = runRepository.findById(runId) ?: throw ReproductionRunNotFoundException(runId)
        val capsule = capsuleRepository.findById(run.capsuleId)
        if (capsule == null || capsule.questionId != questionId) throw ReproductionRunNotFoundException(runId)
        return run
    }

    companion object {
        const val RECENT_RUNS = 10
        const val MAX_TRANSITIONS = 5
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
    private val planner: ReproductionPlanner,
    private val questionRepository: QuestionRepository,
    private val answerRepository: AnswerRepository,
    private val outboxEventRepository: OutboxEventRepository,
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
        val result = ReproductionResult.of(command.buildFailed, command.exitCode)
        val environment = command.environment.trim().take(MAX_ENVIRONMENT).ifEmpty { "unknown" }
        val runner = attestation.runnerEnvironment.take(20)
        val comparison = ReleaseRegression.detect(result, attestation.trustedWorkflow, runner, environment, dependencyVersion, planner.latestRelease(capsule), runRepository.findLatestPerCell(capsuleId))
        val run = runRepository.save(
            ReproductionRun(
                id = null,
                capsuleId = capsuleId,
                result = result,
                exitCode = command.exitCode,
                commitSha = attestation.sha,
                environment = environment,
                runnerEnvironment = runner,
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
                transition = comparison?.transition,
                comparedRunId = comparison?.comparedRun?.id,
                comparedVersion = comparison?.comparedRun?.dependencyVersion,
            ),
        )
        if (comparison != null) announce(capsule, run)
        if (capsule.rerunRequestedAt != null) capsuleRepository.save(capsule.rerunAnswered())
        runRepository.pruneLogs(capsuleId, KEEP_LOGS)
        verificationService?.refresh(capsule.questionId)
        return ReportedRunResult(requireNotNull(run.id), run.result)
    }

    /**
     * Tells the author, watchers and answerers to take another look (Phase 82.2, ADR-0090). It only
     * ever notifies: whether the question is outdated stays a person's call (ADR-0033).
     */
    private fun announce(capsule: ReproductionCapsule, run: ReproductionRun) {
        val question = questionRepository.findById(capsule.questionId) ?: return
        val answerAuthors = answerRepository.findAllByQuestionId(capsule.questionId).map { it.authorId }.distinct()
        outboxEventRepository.save(
            OutboxEvent.create(
                eventType = OutboxEventTypes.RELEASE_REPRODUCTION_CHANGED,
                aggregateType = "QUESTION",
                aggregateId = capsule.questionId,
                payload = """{"questionAuthorId":${question.authorId},"answerAuthorIds":$answerAuthors,"transition":"${run.transition}","runId":${run.id},"packageName":${json(capsule.matrixPackage)},"fromVersion":${json(run.comparedVersion)},"toVersion":${json(run.dependencyVersion)}}""",
            ),
        )
    }

    /** Package names and versions come from people: a quote in one must not break the payload. */
    private fun json(value: String?): String = if (value == null) "null" else "\"" + value.replace("\\", "\\\\").replace("\"", "\\\"") + "\""

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
