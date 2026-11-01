package com.quno.qunobackend.application.engine

import com.quno.qunobackend.domain.common.OutboxEvent
import com.quno.qunobackend.domain.common.OutboxEventRepository
import com.quno.qunobackend.domain.common.OutboxEventTypes
import com.quno.qunobackend.domain.engine.EngineRun
import com.quno.qunobackend.domain.engine.EngineRunRepository
import com.quno.qunobackend.domain.engine.EngineSuggestion
import com.quno.qunobackend.domain.engine.EngineSuggestionKind
import com.quno.qunobackend.domain.engine.EngineSuggestionRepository
import com.quno.qunobackend.domain.engine.EngineSuggestionStatus
import com.quno.qunobackend.domain.engine.QuestionContent
import com.quno.qunobackend.domain.engine.SecretAllowlistRepository
import com.quno.qunobackend.domain.engine.SecretDetector
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.time.Instant

/**
 * Runs the engines over one question version and leaves suggestions — never edits (Phase 78,
 * ADR-0081). Called inside create/revise/fork (same spot as the error-signature indexer) and by
 * the sweep for older questions; an existing run for the same version and engine version makes
 * it a no-op.
 */
@Component
class QuestionEngine(
    private val suggestionRepository: EngineSuggestionRepository,
    private val runRepository: EngineRunRepository,
    private val allowlistRepository: SecretAllowlistRepository,
    private val outboxEventRepository: OutboxEventRepository,
) {
    @Transactional
    fun analyze(questionId: Long, authorId: Long, versionNumber: Int, content: QuestionContent) {
        if (runRepository.exists(questionId, versionNumber, SECRET_SCAN, SECRET_SCAN_VERSION)) return
        val now = Instant.now()
        suggestionRepository.supersedeOpenBefore(questionId, versionNumber, now)

        val allowed = allowlistRepository.findValueHashes(questionId)
        val fields = mapOf("title" to content.title, "body" to content.body, "environment" to content.environment, "logs" to content.logs)
        val findings = fields.flatMap { (field, text) ->
            SecretDetector.find(text).filter { it.valueHash !in allowed }.map { field to it }
        }

        if (findings.isNotEmpty()) {
            val touched = findings.map { it.first }.toSet()
            fun redacted(field: String, text: String?) = if (field in touched) SecretDetector.redact(text, allowed) else null
            val labels = findings.map { it.second.label }.distinct()
            val suggestion = suggestionRepository.save(
                EngineSuggestion(
                    id = null,
                    questionId = questionId,
                    targetVersionNumber = versionNumber,
                    kind = EngineSuggestionKind.SECRET_REDACTION,
                    status = EngineSuggestionStatus.OPEN,
                    engine = SECRET_SCAN,
                    engineVersion = SECRET_SCAN_VERSION,
                    inputHash = content.inputHash(),
                    summary = "비밀값으로 보이는 문자열 ${findings.size}개(${labels.joinToString()})를 공개 화면에서 가렸어요. 원본에서도 지울까요?",
                    evidence = findings.joinToString(",", prefix = """{"rule":"pattern+entropy","findings":[""", postfix = "]}") { (field, finding) ->
                        """{"field":"$field","label":"${finding.label}","valueHash":"${finding.valueHash}"}"""
                    },
                    proposedTitle = redacted("title", content.title),
                    proposedBody = redacted("body", content.body),
                    proposedEnvironment = redacted("environment", content.environment),
                    proposedLogs = redacted("logs", content.logs),
                    envDigest = null,
                    logRef = null,
                    createdAt = now,
                    resolvedAt = null,
                    resolvedBy = null,
                ),
            )
            // Only the author hears about it: telling watchers where a secret sits would defeat the masking.
            outboxEventRepository.save(
                OutboxEvent.create(
                    eventType = OutboxEventTypes.ENGINE_SECRET_DETECTED,
                    aggregateType = "QUESTION",
                    aggregateId = questionId,
                    payload = """{"questionAuthorId":$authorId,"suggestionId":${suggestion.id},"count":${findings.size}}""",
                ),
            )
        }

        runRepository.save(EngineRun(questionId, versionNumber, SECRET_SCAN, SECRET_SCAN_VERSION, content.inputHash(), findings.size, now))
    }

    companion object {
        const val SECRET_SCAN = "secret-scan"

        /** Bump when the detector's rules change so the sweep re-reads every question. */
        const val SECRET_SCAN_VERSION = 1
    }
}
