package com.quno.qunobackend.domain.verification

import com.quno.qunobackend.domain.errorsignature.ErrorSignature
import com.quno.qunobackend.domain.reproduction.ReproductionResult
import com.quno.qunobackend.domain.reproduction.ReproductionRun
import java.time.Duration
import java.time.Instant

/** The public ladder (ADR-0070). L3 and L4 are defined but not reachable yet (ADR-0089). */
enum class VerificationLevel { L0, L1, L2, L3, L4 }

/** What each rung asks for. A check not met is what the page tells the author to do next. */
enum class VerificationCheck {
    ERROR_SIGNATURE,
    ENVIRONMENT_SNAPSHOT,
    EXPECTED_ACTUAL,
    REPRO_STEPS,
    NO_SECRETS,
    REPRODUCTION_CAPSULE,
    TRUSTED_REPRODUCTION,
    SIGNATURE_MATCH,
}

/** Why a level should be read with care. These travel with the level; only STALE lowers its ranking. */
enum class VerificationFlag {
    /** Every trusted run that used to show the failure now passes: fixed, or the environment moved. */
    NOT_REPRODUCING,

    /** Trusted runs fail, but with a different error than the question reports. */
    SIGNATURE_MISMATCH,

    /** L2, but not re-run within [QuestionVerifier.FRESH_FOR]. */
    STALE,

    /** Someone disputed the result and the next run hasn't answered yet. */
    DISPUTED,
}

/** The author's judgement once the cause is known: it stays findable, but isn't a defect report. */
enum class QuestionClassification { MISUSE, NOT_A_BUG }

/** Everything the verdict depends on, gathered by the application layer. */
data class VerificationFacts(
    val title: String,
    val body: String,
    val logs: String?,
    val signatures: List<ErrorSignature>,
    val hasSnapshot: Boolean,
    val unresolvedSecrets: Int,
    val hasCapsule: Boolean,
    val rerunRequested: Boolean,
    /** Newest run per matrix cell of the active capsule. */
    val runs: List<ReproductionRun>,
)

data class VerificationVerdict(
    val level: VerificationLevel,
    val flags: Set<VerificationFlag>,
    val checks: Map<VerificationCheck, Boolean>,
    val reproducedRuns: List<ReproductionRun>,
    val lastReproducedAt: Instant?,
)

/**
 * Decides a question's level (Phase 94.1, ADR-0089). Pure, and run again on every read, so a rule
 * change or the passage of time applies at once.
 *
 * L1 needs the structure a reader can act on. L2 needs a run nobody could have hand-edited — Quno's
 * reusable workflow on a GitHub-hosted runner — that failed **with the reported error**: a run that
 * fails differently proves a different bug (ADR-0070 B.3).
 */
object QuestionVerifier {
    /** A reproduction older than this says little; the level stays, marked STALE. */
    val FRESH_FOR: Duration = Duration.ofDays(90)

    private val EXPECTED = Regex("""(?i)기대|예상|expected""")
    private val ACTUAL = Regex("""(?i)실제|actual""")
    private val STEPS = Regex("""(?im)```|^\s*1[.)]\s|재현|steps? to reproduce""")

    fun evaluate(facts: VerificationFacts, now: Instant): VerificationVerdict {
        val trusted = facts.runs.filter { it.trustedWorkflow && it.runnerEnvironment == "github-hosted" }
        val failing = trusted.filter { it.result == ReproductionResult.REPRODUCED }
        val reproduced = failing.filter { matchesReported(it, facts.signatures) }
        val lastReproducedAt = reproduced.maxOfOrNull { it.finishedAt }

        val checks = linkedMapOf(
            VerificationCheck.ERROR_SIGNATURE to facts.signatures.isNotEmpty(),
            VerificationCheck.ENVIRONMENT_SNAPSHOT to facts.hasSnapshot,
            VerificationCheck.EXPECTED_ACTUAL to (EXPECTED.containsMatchIn(facts.body) && ACTUAL.containsMatchIn(facts.body)),
            VerificationCheck.REPRO_STEPS to (facts.hasCapsule || STEPS.containsMatchIn(facts.body)),
            VerificationCheck.NO_SECRETS to (facts.unresolvedSecrets == 0),
            VerificationCheck.REPRODUCTION_CAPSULE to facts.hasCapsule,
            VerificationCheck.TRUSTED_REPRODUCTION to failing.isNotEmpty(),
            VerificationCheck.SIGNATURE_MATCH to reproduced.isNotEmpty(),
        )
        val structured = L1_CHECKS.all { checks.getValue(it) }
        val level = when {
            structured && reproduced.isNotEmpty() -> VerificationLevel.L2
            structured -> VerificationLevel.L1
            else -> VerificationLevel.L0
        }
        val flags = buildSet {
            if (reproduced.isEmpty() && failing.isEmpty() && trusted.any { it.result == ReproductionResult.NOT_REPRODUCED }) add(VerificationFlag.NOT_REPRODUCING)
            if (reproduced.isEmpty() && failing.isNotEmpty()) add(VerificationFlag.SIGNATURE_MISMATCH)
            if (lastReproducedAt != null && Duration.between(lastReproducedAt, now) > FRESH_FOR) add(VerificationFlag.STALE)
            if (facts.rerunRequested) add(VerificationFlag.DISPUTED)
        }
        return VerificationVerdict(level, flags, checks, reproduced, lastReproducedAt)
    }

    /**
     * The strong key (root cause + top frames) or the message key: a minimal reproduction rarely
     * has the same application frames as the original, so class + normalised message must count.
     */
    private fun matchesReported(run: ReproductionRun, reported: List<ErrorSignature>): Boolean {
        val hashes = reported.mapNotNull { it.signatureHash }.toSet()
        val keys = reported.map { it.messageKey }.toSet()
        return run.signatureHashes.any { it in hashes } || run.messageKeys.any { it in keys }
    }

    private val L1_CHECKS = listOf(
        VerificationCheck.ERROR_SIGNATURE,
        VerificationCheck.ENVIRONMENT_SNAPSHOT,
        VerificationCheck.EXPECTED_ACTUAL,
        VerificationCheck.REPRO_STEPS,
        VerificationCheck.NO_SECRETS,
    )
}

/** The stored half: what ranking reads without re-evaluating, and the author's classification. */
data class VerificationRecord(
    val questionId: Long,
    val level: VerificationLevel,
    val lastReproducedAt: Instant?,
    val classification: QuestionClassification?,
)

/** Port implemented by infrastructure/persistence/jpa/adapter/QuestionVerificationRepositoryAdapter. */
interface QuestionVerificationRepository {
    fun find(questionId: Long): VerificationRecord?

    fun save(record: VerificationRecord)

    /** Questions at L2 or above whose last reproduction is after [freshSince]; absent otherwise. */
    fun findVerifiedLevels(questionIds: Collection<Long>, freshSince: Instant): Map<Long, VerificationLevel>
}
