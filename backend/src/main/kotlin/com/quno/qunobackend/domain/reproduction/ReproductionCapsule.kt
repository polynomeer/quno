package com.quno.qunobackend.domain.reproduction

import java.time.Instant

/** How the reusable workflow builds and runs a capsule (Phase 80, ADR-0087). */
enum class ReproductionKind {
    /** `bash -c <command>`; no build step. */
    SCRIPT,

    /** `./gradlew testClasses`, then `./gradlew <command>` (e.g. `test --tests 'x.PoolTest'`). */
    GRADLE,

    /** `docker build -f <command> .`, then `docker run` of that image. */
    DOCKERFILE,
}

/**
 * What one run showed. The exit code follows `git bisect run`: 0 means the code behaved (the bug
 * did not show), 125 means "can't tell", any other code up to 127 means the bug showed. Above 127
 * the process was killed (signal, timeout), which proves nothing either way.
 */
enum class ReproductionResult {
    REPRODUCED,
    NOT_REPRODUCED,
    BUILD_FAILED,
    INCONCLUSIVE,
    ;

    companion object {
        fun of(buildFailed: Boolean, exitCode: Int?): ReproductionResult = when {
            buildFailed -> BUILD_FAILED
            exitCode == null -> INCONCLUSIVE
            exitCode == 0 -> NOT_REPRODUCED
            exitCode == 125 -> INCONCLUSIVE
            exitCode in 1..127 -> REPRODUCED
            else -> INCONCLUSIVE
        }
    }
}

/**
 * A question's reproduction target (Phase 80, ADR-0087): a public GitHub repository, a ref, and
 * what to run there. One active capsule per question; replacing it supersedes the old one so runs
 * of the old spec never count for the new one.
 */
class ReproductionCapsule private constructor(
    val id: Long?,
    val questionId: Long,
    val repository: String,
    val ref: String,
    val kind: ReproductionKind,
    val command: String,
    val createdBy: Long,
    val createdAt: Instant,
    val supersededAt: Instant?,
    val rerunRequestedAt: Instant?,
    val rerunRequestedBy: Long?,
    val rerunReason: String?,
) {
    val active: Boolean get() = supersededAt == null

    /** A full commit sha pins the capsule: a run of any other commit doesn't count. */
    val pinnedSha: String? get() = ref.takeIf { SHA.matches(it) }?.lowercase()

    fun supersede(): ReproductionCapsule = copy(supersededAt = Instant.now())

    /** "이의 제기": asks the repository's owner to run it again. One outstanding request at a time. */
    fun requestRerun(userId: Long, reason: String): ReproductionCapsule {
        if (rerunRequestedAt != null) throw RerunAlreadyRequestedException(requireNotNull(id))
        val trimmed = reason.trim()
        if (trimmed.isEmpty() || trimmed.length > MAX_REASON) throw InvalidReproductionCapsuleException("reason must be 1..$MAX_REASON characters")
        return copy(rerunRequestedAt = Instant.now(), rerunRequestedBy = userId, rerunReason = trimmed)
    }

    /** A new run answers the outstanding request. */
    fun rerunAnswered(): ReproductionCapsule = copy(rerunRequestedAt = null, rerunRequestedBy = null, rerunReason = null)

    /** Whether a run's reported spec is this capsule's — the workflow reports what it actually ran. */
    fun matches(kind: ReproductionKind, command: String): Boolean = this.kind == kind && this.command == command.trim()

    fun isFrom(repository: String): Boolean = this.repository.equals(repository, ignoreCase = true)

    private fun copy(
        supersededAt: Instant? = this.supersededAt,
        rerunRequestedAt: Instant? = this.rerunRequestedAt,
        rerunRequestedBy: Long? = this.rerunRequestedBy,
        rerunReason: String? = this.rerunReason,
    ) = ReproductionCapsule(id, questionId, repository, ref, kind, command, createdBy, createdAt, supersededAt, rerunRequestedAt, rerunRequestedBy, rerunReason)

    companion object {
        const val MAX_COMMAND = 1000
        const val MAX_REASON = 500
        private val REPOSITORY = Regex("""[A-Za-z0-9](?:[A-Za-z0-9-]{0,38})/[A-Za-z0-9._-]{1,100}""")
        private val REF = Regex("""[A-Za-z0-9._/-]{1,255}""")
        private val SHA = Regex("""(?i)[0-9a-f]{40}""")

        /** [repository] is `owner/name` on github.com; a full `https://github.com/owner/name` URL is accepted too. */
        fun attach(questionId: Long, repository: String, ref: String, kind: ReproductionKind, command: String, createdBy: Long): ReproductionCapsule {
            val normalizedRepository = repository.trim().removePrefix("https://").removePrefix("github.com/").removeSuffix("/").removeSuffix(".git")
            if (!REPOSITORY.matches(normalizedRepository)) throw InvalidReproductionCapsuleException("repository must be owner/name on github.com")
            val normalizedRef = ref.trim()
            if (!REF.matches(normalizedRef) || normalizedRef.contains("..")) throw InvalidReproductionCapsuleException("ref must be a branch, tag or commit sha")
            val normalizedCommand = command.trim()
            if (normalizedCommand.isEmpty() || normalizedCommand.length > MAX_COMMAND) throw InvalidReproductionCapsuleException("command must be 1..$MAX_COMMAND characters")
            if (kind == ReproductionKind.DOCKERFILE && (normalizedCommand.startsWith("/") || normalizedCommand.contains(".."))) {
                throw InvalidReproductionCapsuleException("a Dockerfile path must stay inside the repository")
            }
            return ReproductionCapsule(null, questionId, normalizedRepository, normalizedRef, kind, normalizedCommand, createdBy, Instant.now(), null, null, null, null)
        }

        fun reconstitute(
            id: Long,
            questionId: Long,
            repository: String,
            ref: String,
            kind: ReproductionKind,
            command: String,
            createdBy: Long,
            createdAt: Instant,
            supersededAt: Instant?,
            rerunRequestedAt: Instant?,
            rerunRequestedBy: Long?,
            rerunReason: String?,
        ) = ReproductionCapsule(id, questionId, repository, ref, kind, command, createdBy, createdAt, supersededAt, rerunRequestedAt, rerunRequestedBy, rerunReason)
    }
}

/**
 * One finished run, reported by the workflow and proven by its OIDC token. [environment] is what the
 * workflow says it ran on (`ubuntu24 · java 21.0.5`); everything else about where and what comes
 * from the token.
 */
data class ReproductionRun(
    val id: Long?,
    val capsuleId: Long,
    val result: ReproductionResult,
    val exitCode: Int?,
    val commitSha: String,
    val environment: String,
    val runnerEnvironment: String,
    val trustedWorkflow: Boolean,
    val workflowRef: String,
    val runId: Long,
    val runAttempt: Int,
    val runUrl: String,
    val durationMs: Long?,
    val logTail: String?,
    val maskedSecretCount: Int,
    val finishedAt: Instant,
)

/**
 * What a verified GitHub Actions OIDC token says about the run that holds it. Nothing here comes
 * from the request body.
 */
data class RunAttestation(
    val repository: String,
    val sha: String,
    val runId: Long,
    val runAttempt: Int,
    /** `job_workflow_ref`: the workflow file that ran the job — the reusable one when called through it. */
    val workflowRef: String,
    /** `github-hosted` or `self-hosted`. */
    val runnerEnvironment: String,
    /** Ran through Quno's reusable workflow, so the reported spec and exit code are its own, not hand-written. */
    val trustedWorkflow: Boolean,
)

/** Port implemented by infrastructure/runner/GithubOidcRunAttestationVerifier. */
interface RunAttestationVerifier {
    /** Throws [InvalidRunAttestationException] unless [token] is a valid, unexpired token for Quno. */
    fun verify(token: String): RunAttestation
}

interface ReproductionCapsuleRepository {
    fun save(capsule: ReproductionCapsule): ReproductionCapsule

    fun findById(id: Long): ReproductionCapsule?

    fun findActiveByQuestionId(questionId: Long): ReproductionCapsule?

    /** Row-locks the capsule so a run and a rerun request can't race. */
    fun findByIdForUpdate(id: Long): ReproductionCapsule?
}

interface ReproductionRunRepository {
    fun save(run: ReproductionRun): ReproductionRun

    fun findById(id: Long): ReproductionRun?

    fun exists(capsuleId: Long, runId: Long, runAttempt: Int): Boolean

    /** Newest first. */
    fun findRecentByCapsuleId(capsuleId: Long, limit: Int): List<ReproductionRun>

    /** The newest run per [ReproductionRun.environment]. */
    fun findLatestPerEnvironment(capsuleId: Long): List<ReproductionRun>

    /** Drops the log of all but the newest [keep] runs; their results stay. */
    fun pruneLogs(capsuleId: Long, keep: Int)
}

class InvalidReproductionCapsuleException(message: String) : RuntimeException(message)

class ReproductionCapsuleNotFoundException(message: String) : RuntimeException(message)

class ReproductionRunNotFoundException(runId: Long) : RuntimeException("Reproduction run not found: $runId")

class InvalidRunAttestationException(message: String) : RuntimeException(message)

/** The run doesn't belong to this capsule: other repository, other commit, other spec, or a superseded capsule. */
class ReproductionRunMismatchException(message: String) : RuntimeException(message)

class DuplicateReproductionRunException(runId: Long, runAttempt: Int) : RuntimeException("run $runId attempt $runAttempt was already recorded")

class RerunAlreadyRequestedException(capsuleId: Long) : RuntimeException("a rerun of capsule $capsuleId is already requested")
