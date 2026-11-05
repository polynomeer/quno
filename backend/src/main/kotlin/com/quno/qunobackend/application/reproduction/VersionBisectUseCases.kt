package com.quno.qunobackend.application.reproduction

import com.quno.qunobackend.domain.common.OutboxEvent
import com.quno.qunobackend.domain.common.OutboxEventRepository
import com.quno.qunobackend.domain.common.OutboxEventTypes
import com.quno.qunobackend.domain.question.QuestionAccessDeniedException
import com.quno.qunobackend.domain.question.QuestionNotFoundException
import com.quno.qunobackend.domain.question.QuestionRepository
import com.quno.qunobackend.domain.reproduction.BisectAlreadyRunningException
import com.quno.qunobackend.domain.reproduction.BisectConfidence
import com.quno.qunobackend.domain.reproduction.BisectDecision
import com.quno.qunobackend.domain.reproduction.BisectNotDecidableException
import com.quno.qunobackend.domain.reproduction.BisectNotFoundException
import com.quno.qunobackend.domain.reproduction.BisectObservation
import com.quno.qunobackend.domain.reproduction.BisectState
import com.quno.qunobackend.domain.reproduction.BisectStatus
import com.quno.qunobackend.domain.reproduction.InvalidBisectRequestException
import com.quno.qunobackend.domain.reproduction.PackageReleaseFeed
import com.quno.qunobackend.domain.reproduction.ReleaseListUnavailableException
import com.quno.qunobackend.domain.reproduction.ReproductionBisect
import com.quno.qunobackend.domain.reproduction.ReproductionBisectRepository
import com.quno.qunobackend.domain.reproduction.ReproductionCapsule
import com.quno.qunobackend.domain.reproduction.ReproductionCapsuleNotFoundException
import com.quno.qunobackend.domain.reproduction.ReproductionCapsuleRepository
import com.quno.qunobackend.domain.reproduction.ReproductionResult
import com.quno.qunobackend.domain.reproduction.ReproductionRunRepository
import com.quno.qunobackend.domain.reproduction.VersionBisect
import com.quno.qunobackend.domain.stack.Version
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant

data class VersionBisectResult(
    val id: Long,
    val fromVersion: String,
    val toVersion: String,
    val regression: Boolean,
    val status: BisectStatus,
    val lowVersion: String,
    val highVersion: String,
    val confidence: BisectConfidence?,
    val runsUsed: Int,
    val maxRuns: Int,
    val candidateCount: Int,
    val probes: List<String>,
    val decision: BisectDecision,
    val createdAt: Instant,
    val finishedAt: Instant?,
)

/**
 * The search's life after it starts (Phase 83, ADR-0091): replaying trusted runs into
 * [VersionBisect], closing the search when it has an answer, and the author's decision on it.
 */
@Service
class VersionBisectService(
    private val bisectRepository: ReproductionBisectRepository,
    private val runRepository: ReproductionRunRepository,
    private val capsuleRepository: ReproductionCapsuleRepository,
    private val questionRepository: QuestionRepository,
    private val outboxEventRepository: OutboxEventRepository,
) {
    /** What to run next for a running search, or null if there's no search. */
    fun nextProbes(capsuleId: Long): List<String>? = bisectRepository.findRunning(capsuleId)?.let { state(it).probes }

    fun latest(capsuleId: Long): VersionBisectResult? = bisectRepository.findLatest(capsuleId)?.let { bisect ->
        val live = if (bisect.status == BisectStatus.RUNNING) state(bisect) else null
        bisect.toResult(live)
    }

    /** Called after every recorded run: the run may have settled the search. */
    @Transactional
    fun onRunRecorded(capsuleId: Long, now: Instant = Instant.now()) {
        val bisect = bisectRepository.findRunning(capsuleId) ?: return
        val state = state(bisect)
        val finished = state.status != BisectStatus.RUNNING
        bisectRepository.save(
            bisect.copy(
                status = state.status,
                lowVersion = state.low,
                highVersion = state.high,
                confidence = state.confidence,
                runsUsed = state.runsUsed,
                finishedAt = if (finished) now else null,
            ),
        )
        if (finished) announce(bisect, state)
    }

    /** Replacing a capsule ends its search: the runs it was reading belong to the old spec. */
    @Transactional
    fun cancelFor(capsuleId: Long, now: Instant = Instant.now()) {
        bisectRepository.findRunning(capsuleId)?.let { bisectRepository.save(it.copy(status = BisectStatus.CANCELLED, finishedAt = now)) }
    }

    @Transactional
    fun cancel(questionId: Long, bisectId: Long, actorId: Long, now: Instant = Instant.now()): VersionBisectResult {
        val bisect = owned(questionId, bisectId, actorId)
        if (bisect.status != BisectStatus.RUNNING) throw BisectNotDecidableException(bisectId)
        return bisectRepository.save(bisect.copy(status = BisectStatus.CANCELLED, finishedAt = now)).toResult(null)
    }

    /** Phase 83.2: a boundary is a suggestion until its author approves it. */
    @Transactional
    fun decide(questionId: Long, bisectId: Long, actorId: Long, approve: Boolean): VersionBisectResult {
        val bisect = owned(questionId, bisectId, actorId)
        if (bisect.status != BisectStatus.FOUND) throw BisectNotDecidableException(bisectId)
        return bisectRepository.save(bisect.copy(decision = if (approve) BisectDecision.APPROVED else BisectDecision.REJECTED)).toResult(null)
    }

    private fun owned(questionId: Long, bisectId: Long, actorId: Long): ReproductionBisect {
        val question = questionRepository.findById(questionId) ?: throw QuestionNotFoundException(questionId)
        val bisect = bisectRepository.findById(bisectId) ?: throw BisectNotFoundException(bisectId)
        if (capsuleRepository.findById(bisect.capsuleId)?.questionId != questionId) throw BisectNotFoundException(bisectId)
        if (question.authorId != actorId) throw QuestionAccessDeniedException(questionId)
        return bisect
    }

    private fun state(bisect: ReproductionBisect): BisectState {
        // Only runs a repository owner couldn't have hand-edited decide a version's behaviour (ADR-0089).
        val observations = runRepository.findSince(bisect.capsuleId, bisect.createdAt)
            .filter { it.trustedWorkflow && it.runnerEnvironment == "github-hosted" && it.dependencyVersion != null }
            .sortedBy { it.finishedAt }
            .map { BisectObservation(requireNotNull(it.dependencyVersion), it.result) }
        return VersionBisect.evaluate(bisect.candidates, bisect.fromResult, bisect.toResult, observations)
    }

    private fun announce(bisect: ReproductionBisect, state: BisectState) {
        val questionId = capsuleRepository.findById(bisect.capsuleId)?.questionId ?: return
        outboxEventRepository.save(
            OutboxEvent.create(
                eventType = OutboxEventTypes.VERSION_SEARCH_FINISHED,
                aggregateType = "QUESTION",
                aggregateId = questionId,
                payload = """{"questionAuthorId":${bisect.createdBy},"bisectId":${bisect.id},"status":"${state.status}"}""",
            ),
        )
    }

    private fun ReproductionBisect.toResult(live: BisectState?) = VersionBisectResult(
        id = requireNotNull(id),
        fromVersion = fromVersion,
        toVersion = toVersion,
        regression = regression,
        status = status,
        lowVersion = live?.low ?: lowVersion,
        highVersion = live?.high ?: highVersion,
        confidence = confidence,
        runsUsed = live?.runsUsed ?: runsUsed,
        maxRuns = VersionBisect.MAX_RUNS,
        candidateCount = candidates.size,
        probes = live?.probes.orEmpty(),
        decision = decision,
        createdAt = createdAt,
        finishedAt = finishedAt,
    )
}

/**
 * Starting a search (Phase 83, ADR-0091). The author's call, because every probe spends their
 * repository's CI minutes. Deliberately not `@Transactional`: the registry call stays outside
 * any database transaction, as with the release scan (ADR-0033).
 */
@Service
class StartVersionBisectUseCase(
    private val capsuleRepository: ReproductionCapsuleRepository,
    private val questionRepository: QuestionRepository,
    private val runRepository: ReproductionRunRepository,
    private val releaseFeed: PackageReleaseFeed,
    private val store: VersionBisectStore,
    private val service: VersionBisectService,
) {
    fun execute(questionId: Long, actorId: Long, fromVersion: String, toVersion: String): VersionBisectResult {
        val question = questionRepository.findById(questionId) ?: throw QuestionNotFoundException(questionId)
        if (question.authorId != actorId) throw QuestionAccessDeniedException(questionId)
        val capsule = capsuleRepository.findActiveByQuestionId(questionId) ?: throw ReproductionCapsuleNotFoundException("question $questionId has no reproduction capsule")
        val packageName = capsule.matrixPackage ?: throw InvalidBisectRequestException("a version search needs a matrix package on the capsule")
        val capsuleId = requireNotNull(capsule.id)

        val from = Version.parseOrNull(fromVersion) ?: throw InvalidBisectRequestException("'$fromVersion' is not a version")
        val to = Version.parseOrNull(toVersion) ?: throw InvalidBisectRequestException("'$toVersion' is not a version")
        if (from >= to) throw InvalidBisectRequestException("the range must go from an older version to a newer one")

        fun resultOf(version: Version): ReproductionResult? = runRepository.findLatestPerCell(capsuleId)
            .filter { it.trustedWorkflow && it.runnerEnvironment == "github-hosted" && it.dependencyVersion?.let(Version::parseOrNull) == version }
            .maxByOrNull { it.finishedAt }
            ?.result
            ?.takeIf { it == ReproductionResult.REPRODUCED || it == ReproductionResult.NOT_REPRODUCED }
        val fromResult = resultOf(from) ?: throw InvalidBisectRequestException("$fromVersion has no trusted, conclusive run yet")
        val toResult = resultOf(to) ?: throw InvalidBisectRequestException("$toVersion has no trusted, conclusive run yet")
        if (fromResult == toResult) throw InvalidBisectRequestException("both ends behave the same; there is nothing to find between them")

        val released = releaseFeed.fetchVersions(packageName) ?: throw ReleaseListUnavailableException(packageName)
        val candidates = VersionBisect.candidates(released, fromVersion, toVersion)
        val bisect = store.create(capsule, ReproductionBisect(null, capsuleId, fromVersion, toVersion, fromResult, toResult, candidates, BisectStatus.RUNNING, fromVersion, toVersion, null, 0, BisectDecision.PENDING, actorId, Instant.now(), null))
        // Neighbouring versions need no search beyond confirming each side; the shared path handles that too.
        service.onRunRecorded(capsuleId)
        return requireNotNull(service.latest(capsuleId)) { "search ${bisect.id} just saved" }
    }
}

/** The transactional half of [StartVersionBisectUseCase]. */
@Service
class VersionBisectStore(
    private val capsuleRepository: ReproductionCapsuleRepository,
    private val bisectRepository: ReproductionBisectRepository,
) {
    @Transactional
    fun create(capsule: ReproductionCapsule, bisect: ReproductionBisect): ReproductionBisect {
        val capsuleId = requireNotNull(capsule.id)
        val locked = capsuleRepository.findByIdForUpdate(capsuleId)
        if (locked == null || !locked.active) throw ReproductionCapsuleNotFoundException("capsule $capsuleId was replaced")
        if (bisectRepository.findRunning(capsuleId) != null) throw BisectAlreadyRunningException(capsuleId)
        return bisectRepository.save(bisect)
    }
}
