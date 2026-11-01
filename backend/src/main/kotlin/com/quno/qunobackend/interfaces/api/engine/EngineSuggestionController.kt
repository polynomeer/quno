package com.quno.qunobackend.interfaces.api.engine

import com.quno.qunobackend.application.engine.EngineSuggestionUseCase
import com.quno.qunobackend.domain.engine.EngineSuggestion
import com.quno.qunobackend.domain.engine.EngineSuggestionKind
import com.quno.qunobackend.domain.engine.EngineSuggestionStatus
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.time.Instant

data class EngineSuggestionResponse(
    val id: Long,
    val questionId: Long,
    val targetVersionNumber: Int,
    val kind: EngineSuggestionKind,
    val status: EngineSuggestionStatus,
    val summary: String,
    /** Provenance (ADR-0068): which engine, which version of it, and which input it read. */
    val engine: String,
    val engineVersion: Int,
    val inputHash: String,
    /** Which fields accepting would change. */
    val changedFields: List<String>,
    val proposedTitle: String?,
    val proposedBody: String?,
    val proposedEnvironment: String?,
    val proposedLogs: String?,
    val createdAt: Instant,
    val resolvedAt: Instant?,
)

private fun EngineSuggestion.toResponse() = EngineSuggestionResponse(
    id = requireNotNull(id),
    questionId = questionId,
    targetVersionNumber = targetVersionNumber,
    kind = kind,
    status = status,
    summary = summary,
    engine = engine,
    engineVersion = engineVersion,
    inputHash = inputHash,
    changedFields = listOfNotNull(
        "title".takeIf { proposedTitle != null },
        "body".takeIf { proposedBody != null },
        "environment".takeIf { proposedEnvironment != null },
        "logs".takeIf { proposedLogs != null },
    ),
    proposedTitle = proposedTitle,
    proposedBody = proposedBody,
    proposedEnvironment = proposedEnvironment,
    proposedLogs = proposedLogs,
    createdAt = createdAt,
    resolvedAt = resolvedAt,
)

/** QunoBot suggestions on a question (Phase 78, ADR-0081) — the question's author only. */
@RestController
@RequestMapping("/api/v1")
class EngineSuggestionController(
    private val useCase: EngineSuggestionUseCase,
) {
    @GetMapping("/questions/{questionId}/engine-suggestions")
    fun list(@AuthenticationPrincipal userId: Long, @PathVariable questionId: Long): List<EngineSuggestionResponse> = useCase.list(questionId, userId).map { it.toResponse() }

    @PostMapping("/engine-suggestions/{id}/accept")
    fun accept(@AuthenticationPrincipal userId: Long, @PathVariable id: Long): EngineSuggestionResponse = useCase.accept(id, userId).toResponse()

    @PostMapping("/engine-suggestions/{id}/dismiss")
    fun dismiss(@AuthenticationPrincipal userId: Long, @PathVariable id: Long): EngineSuggestionResponse = useCase.dismiss(id, userId).toResponse()
}
