package com.quno.qunobackend.domain.reproduction

import com.quno.qunobackend.domain.stack.Version
import java.time.Duration
import java.time.Instant

/**
 * Which versions a capsule's matrix runs (Phase 81, ADR-0088). Quno has no release list per
 * package, so it picks from what the question already knows, in this order, up to
 * [ReproductionCapsule.MAX_MATRIX_VERSIONS]:
 *
 * 0. the tracked technology's latest release (Phase 82, ADR-0090), which always gets a cell,
 * 1. versions the author listed on the capsule,
 * 2. the version in the question's stack fingerprint (`^3.2.0` reads as `3.2.0`),
 * 3. the edges of the answers' applicability ranges — where an answer says it starts or stops
 *    holding is exactly where a run says the most.
 *
 * The result is in version order. Without a matrix package there is one cell, `null`.
 */
object ReproductionMatrix {
    fun plan(capsule: ReproductionCapsule, questionVersion: String?, rangeEdges: List<String>, latestRelease: String? = null): List<String?> {
        if (capsule.matrixPackage == null) return listOf(null)
        val picked = linkedMapOf<Version, String>()
        fun add(raw: String?, declared: Boolean = false) {
            if (raw == null || picked.size >= ReproductionCapsule.MAX_MATRIX_VERSIONS) return
            val version = (if (declared) Version.fromDeclaredOrNull(raw) else Version.parseOrNull(raw)) ?: return
            val text = if (declared) raw.trim().trimStart('^', '~', '>', '=', '<', ' ') else raw.trim()
            picked.putIfAbsent(version, text)
        }
        add(latestRelease)
        capsule.matrixVersions.forEach { add(it) }
        add(questionVersion, declared = true)
        rangeEdges.forEach { add(it) }
        return picked.entries.sortedBy { it.key }.map { it.value }.ifEmpty { listOf(null) }
    }
}

/**
 * When a scheduled run is worth doing (Phase 81.2): instead of a queue, the workflow asks before it
 * runs and Quno answers by priority. Manual runs, an open 이의 제기 and a capsule that never ran go
 * at once; otherwise the interval shrinks with how often the question is read.
 */
object ReproductionSchedule {
    private val MANUAL_EVENTS = setOf("workflow_dispatch", "push", "pull_request")

    fun dueReason(eventName: String?, rerunRequested: Boolean, lastRunAt: Instant?, viewCount: Long, now: Instant, releaseUntested: Boolean = false): String? = when {
        eventName in MANUAL_EVENTS -> "manual"
        rerunRequested -> "rerun-requested"
        lastRunAt == null -> "first-run"
        releaseUntested -> "new-release"
        Duration.between(lastRunAt, now) >= intervalFor(viewCount) -> "interval"
        else -> null
    }

    /** 100+ views: daily; 10+: every 3 days; otherwise weekly. */
    fun intervalFor(viewCount: Long): Duration = when {
        viewCount >= 100 -> Duration.ofDays(1)
        viewCount >= 10 -> Duration.ofDays(3)
        else -> Duration.ofDays(7)
    }
}

/** What the workflow should do now. An empty matrix with [due] false means "skip this time". */
data class ReproductionPlan(val due: Boolean, val reason: String, val versions: List<String?>, val nextDueAt: Instant?)

/**
 * Runner health over a window (Phase 81.4). Runs cost Quno nothing on external CI, so "cost" is
 * the runner minutes the repositories spent; flaky cells are cells whose runs of the same commit
 * disagreed (one reproduced, one not).
 */
data class ReproductionMetrics(
    val since: Instant,
    val runs: Long,
    val conclusiveRuns: Long,
    val trustedRuns: Long,
    val runnerMinutes: Long,
    val cells: Long,
    val flakyCells: Long,
    val activeCapsules: Long,
    /** 이의 제기 ever made, open or answered. */
    val disputes: Long,
)

/** How a new release behaves compared with the version before it (Phase 82, ADR-0090). */
enum class ReproductionTransition {
    /** The version before passed, this release fails: "깨짐 감지". */
    BROKE,

    /** The version before failed, this release passes: a candidate fix. */
    FIXED,
}

data class ReleaseComparison(val transition: ReproductionTransition, val comparedRun: ReproductionRun)

/**
 * Whether a run says a release changed behaviour. Only trusted, conclusive runs count, on both
 * sides: a hand-edited exit code or a failed build says nothing about the release. It fires once per
 * cell (the first conclusive run of the release in that environment), and only for the release, so
 * walking a capsule's old versions for the first time never looks like a regression.
 */
object ReleaseRegression {
    private val CONCLUSIVE = setOf(ReproductionResult.REPRODUCED, ReproductionResult.NOT_REPRODUCED)

    fun detect(
        result: ReproductionResult,
        trusted: Boolean,
        runner: String,
        environment: String,
        version: String?,
        latestRelease: String?,
        cellsBefore: List<ReproductionRun>,
    ): ReleaseComparison? {
        if (!trusted || runner != "github-hosted" || result !in CONCLUSIVE) return null
        val parsed = version?.let(Version::parseOrNull) ?: return null
        if (latestRelease == null || parsed != Version.parseOrNull(latestRelease)) return null
        val sameEnvironment = cellsBefore.filter { it.environment == environment && it.trustedWorkflow && it.runnerEnvironment == "github-hosted" && it.result in CONCLUSIVE }
        if (sameEnvironment.any { it.dependencyVersion?.let(Version::parseOrNull) == parsed }) return null
        val previous = sameEnvironment
            .mapNotNull { run -> run.dependencyVersion?.let(Version::parseOrNull)?.takeIf { it < parsed }?.let { it to run } }
            .maxByOrNull { it.first }
            ?.second
            ?: return null
        return when {
            previous.result == result -> null
            result == ReproductionResult.REPRODUCED -> ReleaseComparison(ReproductionTransition.BROKE, previous)
            else -> ReleaseComparison(ReproductionTransition.FIXED, previous)
        }
    }
}
