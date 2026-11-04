package com.quno.qunobackend.domain.reproduction

import com.quno.qunobackend.domain.stack.Version
import java.time.Duration
import java.time.Instant

/**
 * Which versions a capsule's matrix runs (Phase 81, ADR-0088). Quno has no release list per
 * package, so it picks from what the question already knows, in this order, up to
 * [ReproductionCapsule.MAX_MATRIX_VERSIONS]:
 *
 * 1. versions the author listed on the capsule,
 * 2. the version in the question's stack fingerprint (`^3.2.0` reads as `3.2.0`),
 * 3. the edges of the answers' applicability ranges — where an answer says it starts or stops
 *    holding is exactly where a run says the most.
 *
 * The result is in version order. Without a matrix package there is one cell, `null`.
 */
object ReproductionMatrix {
    fun plan(capsule: ReproductionCapsule, questionVersion: String?, rangeEdges: List<String>): List<String?> {
        if (capsule.matrixPackage == null) return listOf(null)
        val picked = linkedMapOf<Version, String>()
        fun add(raw: String?, declared: Boolean = false) {
            if (raw == null || picked.size >= ReproductionCapsule.MAX_MATRIX_VERSIONS) return
            val version = (if (declared) Version.fromDeclaredOrNull(raw) else Version.parseOrNull(raw)) ?: return
            val text = if (declared) raw.trim().trimStart('^', '~', '>', '=', '<', ' ') else raw.trim()
            picked.putIfAbsent(version, text)
        }
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

    fun dueReason(eventName: String?, rerunRequested: Boolean, lastRunAt: Instant?, viewCount: Long, now: Instant): String? = when {
        eventName in MANUAL_EVENTS -> "manual"
        rerunRequested -> "rerun-requested"
        lastRunAt == null -> "first-run"
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
