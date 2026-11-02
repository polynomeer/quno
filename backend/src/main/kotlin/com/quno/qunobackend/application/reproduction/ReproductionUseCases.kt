package com.quno.qunobackend.application.reproduction

import com.quno.qunobackend.domain.engine.SecretDetector
import com.quno.qunobackend.domain.question.QuestionAccessDeniedException
import com.quno.qunobackend.domain.question.QuestionNotFoundException
import com.quno.qunobackend.domain.question.QuestionRepository
import com.quno.qunobackend.domain.reproduction.DuplicateReproductionRunException
import com.quno.qunobackend.domain.reproduction.ReproductionCapsule
import com.quno.qunobackend.domain.reproduction.ReproductionCapsuleNotFoundException
import com.quno.qunobackend.domain.reproduction.ReproductionCapsuleRepository
import com.quno.qunobackend.domain.reproduction.ReproductionKind
import com.quno.qunobackend.domain.reproduction.ReproductionResult
import com.quno.qunobackend.domain.reproduction.ReproductionRun
import com.quno.qunobackend.domain.reproduction.ReproductionRunMismatchException
import com.quno.qunobackend.domain.reproduction.ReproductionRunNotFoundException
import com.quno.qunobackend.domain.reproduction.ReproductionRunRepository
import com.quno.qunobackend.domain.reproduction.RunAttestationVerifier
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
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
)

/** What the question page shows (Phase 80.3): the capsule, the newest run per environment and the recent history. */
data class ReproductionPanelResult(
    val capsule: ReproductionCapsuleResult?,
    val latestByEnvironment: List<ReproductionRunSummary>,
    val recentRuns: List<ReproductionRunSummary>,
)

data class ReproductionRunLogResult(val runId: Long, val logTail: String?, val maskedSecretCount: Int)

data class AttachReproductionCapsuleCommand(
    val questionId: Long,
    val actorId: Long,
    val repository: String,
    val ref: String,
    val kind: ReproductionKind,
    val command: String,
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
)

data class ReportedRunResult(val runId: Long, val result: ReproductionResult)

private fun ReproductionCapsule.toResult() = ReproductionCapsuleResult(requireNotNull(id), repository, ref, kind, command, createdBy, createdAt, rerunRequestedAt, rerunReason)

private fun ReproductionRun.toSummary() = ReproductionRunSummary(requireNotNull(id), result, exitCode, commitSha, environment, runnerEnvironment, trustedWorkflow, runUrl, durationMs, finishedAt, logTail != null)

/**
 * Attaching a capsule (Phase 80, ADR-0087). The question's author owns its reproduction target;
 * replacing it supersedes the old capsule so its runs stop counting.
 */
@Service
class AttachReproductionCapsuleUseCase(
    private val questionRepository: QuestionRepository,
    private val capsuleRepository: ReproductionCapsuleRepository,
) {
    @Transactional
    fun execute(command: AttachReproductionCapsuleCommand): ReproductionCapsuleResult {
        val question = questionRepository.findByIdForUpdate(command.questionId) ?: throw QuestionNotFoundException(command.questionId)
        if (question.authorId != command.actorId) throw QuestionAccessDeniedException(command.questionId)
        val capsule = ReproductionCapsule.attach(command.questionId, command.repository, command.ref, command.kind, command.command, command.actorId)
        capsuleRepository.findActiveByQuestionId(command.questionId)?.let { capsuleRepository.save(it.supersede()) }
        return capsuleRepository.save(capsule).toResult()
    }
}

@Service
class GetReproductionPanelUseCase(
    private val capsuleRepository: ReproductionCapsuleRepository,
    private val runRepository: ReproductionRunRepository,
) {
    fun execute(questionId: Long): ReproductionPanelResult {
        val capsule = capsuleRepository.findActiveByQuestionId(questionId) ?: return ReproductionPanelResult(null, emptyList(), emptyList())
        val capsuleId = requireNotNull(capsule.id)
        return ReproductionPanelResult(
            capsule = capsule.toResult(),
            latestByEnvironment = runRepository.findLatestPerEnvironment(capsuleId).map { it.toSummary() },
            recentRuns = runRepository.findRecentByCapsuleId(capsuleId, RECENT_RUNS).map { it.toSummary() },
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
) {
    @Transactional
    fun execute(attestationToken: String, command: ReportReproductionRunCommand): ReportedRunResult {
        val attestation = verifier.verify(attestationToken)
        val capsule = capsuleRepository.findByIdForUpdate(command.capsuleId)
            ?: throw ReproductionCapsuleNotFoundException("reproduction capsule ${command.capsuleId} not found")
        val capsuleId = requireNotNull(capsule.id)
        if (!capsule.active) throw ReproductionRunMismatchException("capsule $capsuleId was replaced; update the workflow to the new capsule")
        if (!capsule.isFrom(attestation.repository)) throw ReproductionRunMismatchException("capsule $capsuleId belongs to ${capsule.repository}, not ${attestation.repository}")
        capsule.pinnedSha?.let { if (it != attestation.sha) throw ReproductionRunMismatchException("capsule $capsuleId is pinned to $it, the run was ${attestation.sha}") }
        if (!capsule.matches(command.kind, command.command)) throw ReproductionRunMismatchException("the run's kind/command differ from capsule $capsuleId")
        if (runRepository.exists(capsuleId, attestation.runId, attestation.runAttempt)) throw DuplicateReproductionRunException(attestation.runId, attestation.runAttempt)

        val tail = command.logTail?.let(::tailOf)
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
            ),
        )
        if (capsule.rerunRequestedAt != null) capsuleRepository.save(capsule.rerunAnswered())
        runRepository.pruneLogs(capsuleId, KEEP_LOGS)
        return ReportedRunResult(requireNotNull(run.id), run.result)
    }

    /** The last [MAX_LOG_LINES] lines, then at most [MAX_LOG_CHARS] characters from the end. */
    private fun tailOf(log: String): String = log.lines().takeLast(MAX_LOG_LINES).joinToString("\n").takeLast(MAX_LOG_CHARS)

    companion object {
        const val KEEP_LOGS = 20
        const val MAX_LOG_LINES = 200
        const val MAX_LOG_CHARS = 64 * 1024
        const val MAX_ENVIRONMENT = 200
    }
}
