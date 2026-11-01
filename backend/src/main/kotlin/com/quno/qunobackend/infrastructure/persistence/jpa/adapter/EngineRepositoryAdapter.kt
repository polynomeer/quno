package com.quno.qunobackend.infrastructure.persistence.jpa.adapter

import com.quno.qunobackend.domain.engine.EngineRun
import com.quno.qunobackend.domain.engine.EngineRunRepository
import com.quno.qunobackend.domain.engine.EngineSuggestion
import com.quno.qunobackend.domain.engine.EngineSuggestionKind
import com.quno.qunobackend.domain.engine.EngineSuggestionRepository
import com.quno.qunobackend.domain.engine.EngineSuggestionStatus
import com.quno.qunobackend.domain.engine.SecretAllowlistRepository
import com.quno.qunobackend.infrastructure.persistence.jpa.entity.EngineRunJpaEntity
import com.quno.qunobackend.infrastructure.persistence.jpa.entity.EngineSecretAllowlistJpaEntity
import com.quno.qunobackend.infrastructure.persistence.jpa.entity.EngineSuggestionJpaEntity
import com.quno.qunobackend.infrastructure.persistence.jpa.repository.EngineRunJpaRepository
import com.quno.qunobackend.infrastructure.persistence.jpa.repository.EngineSecretAllowlistJpaRepository
import com.quno.qunobackend.infrastructure.persistence.jpa.repository.EngineSuggestionJpaRepository
import org.springframework.stereotype.Component
import java.time.Instant

@Component
class EngineRepositoryAdapter(
    private val suggestions: EngineSuggestionJpaRepository,
    private val runs: EngineRunJpaRepository,
    private val allowlist: EngineSecretAllowlistJpaRepository,
) : EngineSuggestionRepository,
    EngineRunRepository,
    SecretAllowlistRepository {

    override fun save(suggestion: EngineSuggestion): EngineSuggestion = suggestions.save(
        EngineSuggestionJpaEntity(
            id = suggestion.id,
            questionId = suggestion.questionId,
            targetVersionNumber = suggestion.targetVersionNumber,
            kind = suggestion.kind.name,
            status = suggestion.status.name,
            engine = suggestion.engine,
            engineVersion = suggestion.engineVersion,
            inputHash = suggestion.inputHash,
            summary = suggestion.summary,
            evidence = suggestion.evidence,
            proposedTitle = suggestion.proposedTitle,
            proposedBody = suggestion.proposedBody,
            proposedEnvironment = suggestion.proposedEnvironment,
            proposedLogs = suggestion.proposedLogs,
            envDigest = suggestion.envDigest,
            logRef = suggestion.logRef,
            createdAt = suggestion.createdAt,
            resolvedAt = suggestion.resolvedAt,
            resolvedBy = suggestion.resolvedBy,
        ),
    ).toDomain()

    override fun findById(id: Long): EngineSuggestion? = suggestions.findById(id).orElse(null)?.toDomain()

    override fun findByQuestionId(questionId: Long): List<EngineSuggestion> = suggestions.findAllByQuestionIdOrderByIdDesc(questionId).map { it.toDomain() }

    override fun supersedeOpenBefore(questionId: Long, versionNumber: Int, at: Instant) = suggestions.supersedeOpenBefore(questionId, versionNumber, at)

    override fun save(run: EngineRun) {
        runs.save(
            EngineRunJpaEntity(
                questionId = run.questionId,
                versionNumber = run.versionNumber,
                engine = run.engine,
                engineVersion = run.engineVersion,
                inputHash = run.inputHash,
                findingCount = run.findingCount,
                ranAt = run.ranAt,
            ),
        )
    }

    override fun exists(questionId: Long, versionNumber: Int, engine: String, engineVersion: Int): Boolean = runs.existsByQuestionIdAndVersionNumberAndEngineAndEngineVersion(questionId, versionNumber, engine, engineVersion)

    override fun findQuestionIdsNeedingRun(engine: String, engineVersion: Int, limit: Int): List<Long> = runs.findQuestionIdsNeedingRun(engine, engineVersion, limit)

    override fun findValueHashes(questionId: Long): Set<String> = allowlist.findAllByQuestionId(questionId).map { it.valueHash }.toSet()

    override fun add(questionId: Long, valueHashes: Collection<String>) {
        val existing = findValueHashes(questionId)
        val now = Instant.now()
        allowlist.saveAll(valueHashes.toSet().minus(existing).map { EngineSecretAllowlistJpaEntity(questionId = questionId, valueHash = it, createdAt = now) })
    }

    private fun EngineSuggestionJpaEntity.toDomain() = EngineSuggestion(
        id = id,
        questionId = questionId,
        targetVersionNumber = targetVersionNumber,
        kind = EngineSuggestionKind.valueOf(kind),
        status = EngineSuggestionStatus.valueOf(status),
        engine = engine,
        engineVersion = engineVersion,
        inputHash = inputHash,
        summary = summary,
        evidence = evidence,
        proposedTitle = proposedTitle,
        proposedBody = proposedBody,
        proposedEnvironment = proposedEnvironment,
        proposedLogs = proposedLogs,
        envDigest = envDigest,
        logRef = logRef,
        createdAt = createdAt,
        resolvedAt = resolvedAt,
        resolvedBy = resolvedBy,
    )
}
