package com.quno.qunobackend.infrastructure.persistence.jpa.adapter

import com.quno.qunobackend.domain.reproduction.ReproductionCapsule
import com.quno.qunobackend.domain.reproduction.ReproductionCapsuleRepository
import com.quno.qunobackend.domain.reproduction.ReproductionMetrics
import com.quno.qunobackend.domain.reproduction.ReproductionRun
import com.quno.qunobackend.domain.reproduction.ReproductionRunRepository
import com.quno.qunobackend.infrastructure.persistence.jpa.entity.ReproductionCapsuleJpaEntity
import com.quno.qunobackend.infrastructure.persistence.jpa.entity.ReproductionRunJpaEntity
import com.quno.qunobackend.infrastructure.persistence.jpa.repository.ReproductionCapsuleJpaRepository
import com.quno.qunobackend.infrastructure.persistence.jpa.repository.ReproductionRunJpaRepository
import org.springframework.data.domain.Limit
import org.springframework.stereotype.Component
import java.time.Instant

@Component
class ReproductionCapsuleRepositoryAdapter(
    private val jpaRepository: ReproductionCapsuleJpaRepository,
) : ReproductionCapsuleRepository {

    override fun save(capsule: ReproductionCapsule): ReproductionCapsule = jpaRepository.saveAndFlush(capsule.toEntity()).toDomain()

    override fun findById(id: Long): ReproductionCapsule? = jpaRepository.findById(id).map { it.toDomain() }.orElse(null)

    override fun findActiveByQuestionId(questionId: Long): ReproductionCapsule? = jpaRepository.findByQuestionIdAndSupersededAtIsNull(questionId)?.toDomain()

    override fun findByIdForUpdate(id: Long): ReproductionCapsule? = jpaRepository.findByIdForUpdate(id)?.toDomain()

    private fun ReproductionCapsule.toEntity() = ReproductionCapsuleJpaEntity(
        id = id,
        questionId = questionId,
        repository = repository,
        ref = ref,
        kind = kind,
        command = command,
        createdBy = createdBy,
        createdAt = createdAt,
        supersededAt = supersededAt,
        rerunRequestedAt = rerunRequestedAt,
        rerunRequestedBy = rerunRequestedBy,
        rerunReason = rerunReason,
        matrixPackage = matrixPackage,
        matrixVersions = matrixVersions.takeIf { it.isNotEmpty() }?.joinToString(","),
        rerunRequestCount = rerunRequestCount,
    )

    private fun ReproductionCapsuleJpaEntity.toDomain() = ReproductionCapsule.reconstitute(
        id = requireNotNull(id),
        questionId = questionId,
        repository = repository,
        ref = ref,
        kind = kind,
        command = command,
        createdBy = createdBy,
        createdAt = createdAt,
        supersededAt = supersededAt,
        rerunRequestedAt = rerunRequestedAt,
        rerunRequestedBy = rerunRequestedBy,
        rerunReason = rerunReason,
        matrixPackage = matrixPackage,
        matrixVersions = matrixVersions?.split(',')?.filter { it.isNotEmpty() }.orEmpty(),
        rerunRequestCount = rerunRequestCount,
    )
}

@Component
class ReproductionRunRepositoryAdapter(
    private val jpaRepository: ReproductionRunJpaRepository,
) : ReproductionRunRepository {

    override fun save(run: ReproductionRun): ReproductionRun = jpaRepository.save(run.toEntity()).toDomain()

    override fun findById(id: Long): ReproductionRun? = jpaRepository.findById(id).map { it.toDomain() }.orElse(null)

    override fun exists(capsuleId: Long, runId: Long, runAttempt: Int): Boolean = jpaRepository.existsByCapsuleIdAndRunIdAndRunAttempt(capsuleId, runId, runAttempt)

    override fun findRecentByCapsuleId(capsuleId: Long, limit: Int): List<ReproductionRun> = jpaRepository.findByCapsuleIdOrderByFinishedAtDescIdDesc(capsuleId, Limit.of(limit)).map { it.toDomain() }

    override fun findLatestPerCell(capsuleId: Long): List<ReproductionRun> = jpaRepository.findLatestPerCell(capsuleId).map { it.toDomain() }.sortedByDescending { it.finishedAt }

    override fun lastFinishedAt(capsuleId: Long): Instant? = jpaRepository.lastFinishedAt(capsuleId)

    override fun metrics(since: Instant): ReproductionMetrics = jpaRepository.metrics(since).let {
        ReproductionMetrics(
            since = since,
            runs = it.getRuns(),
            conclusiveRuns = it.getConclusiveRuns(),
            trustedRuns = it.getTrustedRuns(),
            runnerMinutes = it.getRunnerMinutes(),
            cells = it.getCells(),
            flakyCells = it.getFlakyCells(),
            activeCapsules = it.getActiveCapsules(),
            disputes = it.getDisputes(),
        )
    }

    override fun pruneLogs(capsuleId: Long, keep: Int) = jpaRepository.pruneLogs(capsuleId, keep)

    private fun ReproductionRun.toEntity() = ReproductionRunJpaEntity(
        id = id,
        capsuleId = capsuleId,
        result = result,
        exitCode = exitCode,
        commitSha = commitSha,
        environment = environment,
        runnerEnvironment = runnerEnvironment,
        trustedWorkflow = trustedWorkflow,
        workflowRef = workflowRef,
        runId = runId,
        runAttempt = runAttempt,
        runUrl = runUrl,
        durationMs = durationMs,
        logTail = logTail,
        maskedSecretCount = maskedSecretCount,
        finishedAt = finishedAt,
        dependencyVersion = dependencyVersion,
        eventName = eventName,
        signatureHashes = signatureHashes.takeIf { it.isNotEmpty() }?.joinToString(","),
        messageKeys = messageKeys.takeIf { it.isNotEmpty() }?.joinToString(","),
        exceptionClass = exceptionClass,
    )

    private fun ReproductionRunJpaEntity.toDomain() = ReproductionRun(
        id = id,
        capsuleId = capsuleId,
        result = result,
        exitCode = exitCode,
        commitSha = commitSha,
        environment = environment,
        runnerEnvironment = runnerEnvironment,
        trustedWorkflow = trustedWorkflow,
        workflowRef = workflowRef,
        runId = runId,
        runAttempt = runAttempt,
        runUrl = runUrl,
        durationMs = durationMs,
        logTail = logTail,
        maskedSecretCount = maskedSecretCount,
        finishedAt = finishedAt,
        dependencyVersion = dependencyVersion,
        eventName = eventName,
        signatureHashes = signatureHashes?.split(',').orEmpty(),
        messageKeys = messageKeys?.split(',').orEmpty(),
        exceptionClass = exceptionClass,
    )
}
