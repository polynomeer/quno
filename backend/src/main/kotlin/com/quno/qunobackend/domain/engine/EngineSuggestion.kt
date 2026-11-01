package com.quno.qunobackend.domain.engine

import java.security.MessageDigest
import java.time.Instant

/** What an engine proposes: secrets (Phase 78.4, ADR-0081) and formatting cleanups (Phase 78.5, ADR-0082). */
enum class EngineSuggestionKind { SECRET_REDACTION, ADD_CODE_FENCE_LANGUAGE, FENCE_STACK_TRACE, MOVE_LONG_LOG }

enum class EngineSuggestionStatus { OPEN, ACCEPTED, DISMISSED, SUPERSEDED }

class EngineSuggestionNotFoundException(id: Long) : RuntimeException("engine suggestion $id not found")

class EngineSuggestionNotOpenException(id: Long, status: EngineSuggestionStatus) : RuntimeException("engine suggestion $id is already $status")

/** The question was revised after the suggestion was made, so it no longer applies as written. */
class EngineSuggestionOutdatedException(id: Long) : RuntimeException("engine suggestion $id targets an older version of the question")

/** The fields an engine reads, and what a suggestion would write back. */
data class QuestionContent(val title: String, val body: String, val environment: String?, val logs: String?) {
    /** Provenance: which input the engine saw (ADR-0068 출처 필드). */
    fun inputHash(): String = MessageDigest.getInstance("SHA-256")
        .digest(listOf(title, body, environment.orEmpty(), logs.orEmpty()).joinToString("\u0000").toByteArray())
        .joinToString("") { "%02x".format(it) }
}

/**
 * One proposed change to one version of a question. The original stays untouched; accepting
 * becomes a revision under the author's name. [proposed] fields left null keep the current value.
 * [envDigest]/[logRef] belong to engines that execute code (Phase 80) and are null here.
 */
data class EngineSuggestion(
    val id: Long?,
    val questionId: Long,
    val targetVersionNumber: Int,
    val kind: EngineSuggestionKind,
    val status: EngineSuggestionStatus,
    val engine: String,
    val engineVersion: Int,
    val inputHash: String,
    val summary: String,
    /** JSON; for SECRET_REDACTION `{"findings":[{"label":..,"valueHash":..,"field":..}]}`. */
    val evidence: String,
    val proposedTitle: String?,
    val proposedBody: String?,
    val proposedEnvironment: String?,
    val proposedLogs: String?,
    val envDigest: String?,
    val logRef: String?,
    val createdAt: Instant,
    val resolvedAt: Instant?,
    val resolvedBy: Long?,
) {
    fun resolve(status: EngineSuggestionStatus, by: Long?, at: Instant = Instant.now()): EngineSuggestion {
        if (this.status != EngineSuggestionStatus.OPEN) throw EngineSuggestionNotOpenException(requireNotNull(id), this.status)
        return copy(status = status, resolvedAt = at, resolvedBy = by)
    }
}

/** Port implemented by infrastructure/persistence/jpa/adapter/EngineRepositoryAdapter. */
interface EngineSuggestionRepository {
    fun save(suggestion: EngineSuggestion): EngineSuggestion

    fun findById(id: Long): EngineSuggestion?

    /** Newest first. */
    fun findByQuestionId(questionId: Long): List<EngineSuggestion>

    /** Open suggestions made for versions before [versionNumber] become SUPERSEDED. */
    fun supersedeOpenBefore(questionId: Long, versionNumber: Int, at: Instant)
}

/** One analysis of one question version by one engine version — the idempotency key and backfill marker. */
data class EngineRun(
    val questionId: Long,
    val versionNumber: Int,
    val engine: String,
    val engineVersion: Int,
    val inputHash: String,
    val findingCount: Int,
    val ranAt: Instant,
)

interface EngineRunRepository {
    fun save(run: EngineRun)

    fun exists(questionId: Long, versionNumber: Int, engine: String, engineVersion: Int): Boolean

    /** Questions whose latest version has no run of [engine]@[engineVersion] yet, oldest first. */
    fun findQuestionIdsNeedingRun(engine: String, engineVersion: Int, limit: Int): List<Long>
}

/** Value hashes the question's author said are not secrets (dismissing a redaction suggestion, ADR-0081). */
interface SecretAllowlistRepository {
    fun findValueHashes(questionId: Long): Set<String>

    fun add(questionId: Long, valueHashes: Collection<String>)
}
