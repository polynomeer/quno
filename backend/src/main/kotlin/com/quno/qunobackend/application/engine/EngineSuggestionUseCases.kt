package com.quno.qunobackend.application.engine

import com.quno.qunobackend.application.question.dto.ReviseQuestionCommand
import com.quno.qunobackend.application.question.usecase.ReviseQuestionUseCase
import com.quno.qunobackend.domain.engine.EngineRunRepository
import com.quno.qunobackend.domain.engine.EngineSuggestion
import com.quno.qunobackend.domain.engine.EngineSuggestionKind
import com.quno.qunobackend.domain.engine.EngineSuggestionNotFoundException
import com.quno.qunobackend.domain.engine.EngineSuggestionOutdatedException
import com.quno.qunobackend.domain.engine.EngineSuggestionRepository
import com.quno.qunobackend.domain.engine.EngineSuggestionStatus
import com.quno.qunobackend.domain.engine.QuestionContent
import com.quno.qunobackend.domain.engine.SecretAllowlistRepository
import com.quno.qunobackend.domain.question.Question
import com.quno.qunobackend.domain.question.QuestionAccessDeniedException
import com.quno.qunobackend.domain.question.QuestionNotFoundException
import com.quno.qunobackend.domain.question.QuestionRepository
import com.quno.qunobackend.domain.question.QuestionVersion
import com.quno.qunobackend.domain.question.QuestionVersionRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant

/**
 * The author's side of the engine layer (Phase 78, ADR-0081): only the question's author sees
 * suggestions and decides on them. Accepting goes through the ordinary revision path under the
 * author's name; dismissing a redaction means "not a secret" and lifts the masking for good.
 */
@Service
class EngineSuggestionUseCase(
    private val suggestionRepository: EngineSuggestionRepository,
    private val allowlistRepository: SecretAllowlistRepository,
    private val questionRepository: QuestionRepository,
    private val questionVersionRepository: QuestionVersionRepository,
    private val reviseQuestionUseCase: ReviseQuestionUseCase,
) {
    fun list(questionId: Long, viewerId: Long): List<EngineSuggestion> {
        authorOnly(questionId, viewerId)
        return suggestionRepository.findByQuestionId(questionId)
    }

    @Transactional
    fun accept(suggestionId: Long, actorId: Long): EngineSuggestion {
        val suggestion = suggestionRepository.findById(suggestionId) ?: throw EngineSuggestionNotFoundException(suggestionId)
        val (question, latest) = authorOnly(suggestion.questionId, actorId)
        if (suggestion.status == EngineSuggestionStatus.OPEN && latest.versionNumber != suggestion.targetVersionNumber) {
            suggestionRepository.save(suggestion.resolve(EngineSuggestionStatus.SUPERSEDED, by = null))
            throw EngineSuggestionOutdatedException(suggestionId)
        }
        val accepted = suggestionRepository.save(suggestion.resolve(EngineSuggestionStatus.ACCEPTED, by = actorId))
        reviseQuestionUseCase.execute(
            ReviseQuestionCommand(
                questionId = requireNotNull(question.id),
                actorId = actorId,
                title = suggestion.proposedTitle ?: latest.title,
                body = suggestion.proposedBody ?: latest.bodyMarkdown,
                environment = suggestion.proposedEnvironment ?: latest.environment,
                logs = suggestion.proposedLogs ?: latest.logs,
            ),
        )
        return accepted
    }

    @Transactional
    fun dismiss(suggestionId: Long, actorId: Long): EngineSuggestion {
        val suggestion = suggestionRepository.findById(suggestionId) ?: throw EngineSuggestionNotFoundException(suggestionId)
        authorOnly(suggestion.questionId, actorId)
        val dismissed = suggestionRepository.save(suggestion.resolve(EngineSuggestionStatus.DISMISSED, by = actorId, at = Instant.now()))
        if (suggestion.kind == EngineSuggestionKind.SECRET_REDACTION) {
            allowlistRepository.add(suggestion.questionId, VALUE_HASH.findAll(suggestion.evidence).map { it.groupValues[1] }.toList())
        }
        return dismissed
    }

    private fun authorOnly(questionId: Long, viewerId: Long): Pair<Question, QuestionVersion> {
        val question = questionRepository.findById(questionId) ?: throw QuestionNotFoundException(questionId)
        if (question.authorId != viewerId) throw QuestionAccessDeniedException(questionId)
        val latest = questionVersionRepository.findById(requireNotNull(question.latestVersionId)) ?: throw QuestionNotFoundException(questionId)
        return question to latest
    }

    companion object {
        private val VALUE_HASH = Regex(""""valueHash":"([0-9a-f]{64})"""")
    }
}

/** Backfill (ADR-0081): questions the current engine version hasn't read yet, a batch at a time, each in its own transaction. */
@Service
class SweepQuestionEnginesUseCase(
    private val questionEngine: QuestionEngine,
    private val runRepository: EngineRunRepository,
    private val questionRepository: QuestionRepository,
    private val questionVersionRepository: QuestionVersionRepository,
) {
    fun execute(limit: Int): Int {
        val ids = runRepository.findQuestionIdsNeedingRun(QuestionEngine.SECRET_SCAN, QuestionEngine.SECRET_SCAN_VERSION, limit)
        ids.forEach { id ->
            val question = questionRepository.findById(id) ?: return@forEach
            val version = question.latestVersionId?.let(questionVersionRepository::findById) ?: return@forEach
            questionEngine.analyze(id, question.authorId, version.versionNumber, version.toContent())
        }
        return ids.size
    }
}

fun QuestionVersion.toContent() = QuestionContent(title, bodyMarkdown, environment, logs)
