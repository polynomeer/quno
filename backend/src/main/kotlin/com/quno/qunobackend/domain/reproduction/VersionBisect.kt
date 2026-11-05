package com.quno.qunobackend.domain.reproduction

import com.quno.qunobackend.domain.stack.Version
import java.time.Instant

enum class BisectStatus {
    RUNNING,

    /** Two neighbouring released versions behave differently: that's the boundary. */
    FOUND,

    /** Every version left in the range failed to give an answer, e.g. they don't build. */
    UNRESOLVED,

    /** The run budget ran out; the narrowed range is what's known. */
    BUDGET_REACHED,

    CANCELLED,
}

/** How far to trust a boundary (ADR-0091). Runs are never perfectly repeatable, so it's always stated. */
enum class BisectConfidence {
    /** Both sides confirmed by a second run, and every version agrees with the boundary. */
    HIGH,

    /** Consistent, but a side rests on one run. */
    MEDIUM,

    /** Some version gave both answers, or a version above the boundary behaves like one below it. */
    LOW,
}

/** Suggestion, not fact (Phase 83.2): nothing reads a boundary as true until its author says so. */
enum class BisectDecision { PENDING, APPROVED, REJECTED }

/** One trusted run of a candidate version. */
data class BisectObservation(val version: String, val result: ReproductionResult)

data class BisectState(
    val status: BisectStatus,
    /** Versions to run next; empty unless RUNNING. */
    val probes: List<String>,
    /** The highest version known to behave like the start of the range. */
    val low: String,
    /** The lowest version known to behave like the end of the range. */
    val high: String,
    val confidence: BisectConfidence?,
    val runsUsed: Int,
)

/**
 * The search itself (Phase 83, ADR-0091). Pure: it replays every observation on each call, so a
 * late or repeated report just refines the answer and there is no search state to corrupt.
 *
 * Finds the first version in [candidates] (sorted, both ends included) that gives the end's result.
 * Each round probes up to [MAX_PROBES] evenly spaced versions, because a scheduled workflow runs once
 * a day and a job per probe costs less wall-clock than a day per probe. When the boundary is found,
 * each side runs a second time so a single flaky run can't decide it.
 */
object VersionBisect {
    const val MAX_RUNS = 10
    const val MAX_PROBES = 3
    private const val CONFIRMATIONS = 2

    fun evaluate(candidates: List<String>, startResult: ReproductionResult, endResult: ReproductionResult, observations: List<BisectObservation>): BisectState {
        val index = candidates.mapIndexedNotNull { i, raw -> Version.parseOrNull(raw)?.let { it to i } }.toMap()
        val seen = observations.mapNotNull { o -> Version.parseOrNull(o.version)?.let(index::get)?.let { it to o.result } }
        val verdicts = Array(candidates.size) { mutableListOf<ReproductionResult>() }
        verdicts[0] += startResult
        verdicts[candidates.lastIndex] += endResult
        seen.forEach { (i, result) -> if (result == startResult || result == endResult) verdicts[i] += result }
        val runsUsed = seen.size

        // The newest verdict of each version decides where it sits.
        val current = verdicts.map { it.lastOrNull() }
        val highIndex = current.indexOfFirst { it == endResult }.takeIf { it >= 0 } ?: candidates.lastIndex
        val lowIndex = (highIndex - 1 downTo 0).firstOrNull { current[it] == startResult } ?: 0
        val low = candidates[lowIndex]
        val high = candidates[highIndex]

        val tried = seen.map { it.first }.toSet()
        val untried = (lowIndex + 1 until highIndex).filter { it !in tried }
        val budget = MAX_RUNS - runsUsed

        fun state(status: BisectStatus, probes: List<Int> = emptyList(), confidence: BisectConfidence? = null) = BisectState(status, probes.map(candidates::get), low, high, confidence, runsUsed)

        if (untried.isNotEmpty()) {
            return if (budget <= 0) state(BisectStatus.BUDGET_REACHED) else state(BisectStatus.RUNNING, spaced(untried, minOf(MAX_PROBES, budget)))
        }
        if (highIndex - lowIndex > 1) return state(BisectStatus.UNRESOLVED)

        val needsConfirming = listOf(lowIndex, highIndex).filter { verdicts[it].size < CONFIRMATIONS }
        if (needsConfirming.isNotEmpty() && budget > 0) return state(BisectStatus.RUNNING, needsConfirming.take(budget))

        val contradicted = verdicts.any { it.distinct().size > 1 } || (highIndex + 1..candidates.lastIndex).any { current[it] == startResult }
        val confidence = when {
            contradicted -> BisectConfidence.LOW
            needsConfirming.isEmpty() -> BisectConfidence.HIGH
            else -> BisectConfidence.MEDIUM
        }
        return state(BisectStatus.FOUND, confidence = confidence)
    }

    /** Splits [indices] into equal parts: one probe is the midpoint, three are the quartiles. */
    private fun spaced(indices: List<Int>, count: Int): List<Int> = (1..count).map { indices[it * indices.size / (count + 1)] }.distinct()

    /**
     * The versions a search walks: stable releases strictly between the ends, plus the ends. Pre-releases
     * are noise here, since they aren't what anyone upgrades to.
     */
    fun candidates(released: List<String>, from: String, to: String): List<String> {
        val low = requireNotNull(Version.parseOrNull(from))
        val high = requireNotNull(Version.parseOrNull(to))
        val between = released.filter { STABLE.matches(it) }
            .mapNotNull { raw -> Version.parseOrNull(raw)?.takeIf { it > low && it < high }?.let { it to raw } }
            .distinctBy { it.first }
            .sortedBy { it.first }
            .map { it.second }
        return listOf(from) + between + to
    }

    private val STABLE = Regex("""v?\d+(\.\d+)*(\.(RELEASE|Final|GA))?""")
}

/** One search over one capsule's versions. */
data class ReproductionBisect(
    val id: Long?,
    val capsuleId: Long,
    val fromVersion: String,
    val toVersion: String,
    val fromResult: ReproductionResult,
    val toResult: ReproductionResult,
    val candidates: List<String>,
    val status: BisectStatus,
    val lowVersion: String,
    val highVersion: String,
    val confidence: BisectConfidence?,
    val runsUsed: Int,
    val decision: BisectDecision,
    val createdBy: Long,
    val createdAt: Instant,
    val finishedAt: Instant?,
) {
    /** The bug appeared within the range; otherwise it went away. */
    val regression: Boolean get() = toResult == ReproductionResult.REPRODUCED
}

/** Port implemented by infrastructure/external/PackageRegistryReleaseFeed. */
interface PackageReleaseFeed {
    /** Every version the registry knows for [packageName], or null when it can't say. */
    fun fetchVersions(packageName: String): List<String>?
}

interface ReproductionBisectRepository {
    fun save(bisect: ReproductionBisect): ReproductionBisect

    fun findById(id: Long): ReproductionBisect?

    fun findRunning(capsuleId: Long): ReproductionBisect?

    /** The most recent search of the capsule, finished or not. */
    fun findLatest(capsuleId: Long): ReproductionBisect?

    /** The newest search whose boundary its author approved. */
    fun findLatestApproved(capsuleId: Long): ReproductionBisect?
}

class InvalidBisectRequestException(message: String) : RuntimeException(message)

class BisectAlreadyRunningException(capsuleId: Long) : RuntimeException("capsule $capsuleId already has a running search")

class BisectNotFoundException(id: Long) : RuntimeException("Version search not found: $id")

class ReleaseListUnavailableException(packageName: String) : RuntimeException("couldn't read the release list of $packageName")

class BisectNotDecidableException(id: Long) : RuntimeException("version search $id has no boundary to decide on")
