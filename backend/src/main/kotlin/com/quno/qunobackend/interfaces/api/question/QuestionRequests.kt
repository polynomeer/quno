package com.quno.qunobackend.interfaces.api.question

import com.quno.qunobackend.application.question.dto.PromotionDraft
import com.quno.qunobackend.domain.stack.Ecosystem
import com.quno.qunobackend.domain.stack.StackDependency
import jakarta.validation.Valid
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Size
import tools.jackson.databind.JsonNode

data class CreateQuestionRequest(
    @field:NotBlank @field:Size(max = 300)
    val title: String,
    @field:NotBlank
    val body: String,
    val environment: String? = null,
    val logs: String? = null,
    val tags: List<String> = emptyList(),
    /** Entries kept from the stack fingerprint preview (ADR-0076). */
    @field:Valid @field:Size(max = StackDependency.MAX_PER_QUESTION)
    val dependencies: List<DependencyRequest> = emptyList(),
    /** A `quno.snapshot` document from the collector or typed by hand (ADR-0083). */
    val environmentSnapshot: JsonNode? = null,
    /** Ask one verified organization privately (Phase 79, ADR-0085); omitted = public. */
    val organizationId: Long? = null,
)

/** One stack fingerprint entry; limits mirror [StackDependency] so bad input is a 400, not a 500. */
data class DependencyRequest(
    @field:NotNull
    val ecosystem: Ecosystem,
    @field:NotBlank @field:Size(max = StackDependency.MAX_NAME_LENGTH)
    val name: String,
    @field:Size(max = StackDependency.MAX_VERSION_LENGTH)
    val version: String? = null,
) {
    fun toDomain() = StackDependency(ecosystem, name.trim(), version?.trim()?.takeIf { it.isNotEmpty() })
}

data class ParseDependencyFileRequest(
    @field:Size(max = 255)
    val fileName: String? = null,
    @field:NotBlank @field:Size(max = 100_000)
    val content: String,
)

/** Revising a question doesn't touch its tags in MVP — only content fields. */
data class QuestionContentRequest(
    @field:NotBlank @field:Size(max = 300)
    val title: String,
    @field:NotBlank
    val body: String,
    val environment: String? = null,
    val logs: String? = null,
    /** Omit to keep the current stack fingerprint; send [] to clear it (ADR-0076). */
    @field:Valid @field:Size(max = StackDependency.MAX_PER_QUESTION)
    val dependencies: List<DependencyRequest>? = null,
    /** Omit to keep the current environment snapshot; a document replaces it (ADR-0083). */
    val environmentSnapshot: JsonNode? = null,
)

data class MarkQuestionOutdatedRequest(
    @field:NotBlank
    val reason: String,
)

/** The public card a private question becomes (Phase 79.3, ADR-0086). Same limits as a new question. */
data class PromoteQuestionRequest(
    @field:NotBlank @field:Size(max = 300)
    val title: String,
    @field:NotBlank
    val body: String,
    val environment: String? = null,
    val logs: String? = null,
    val tags: List<String> = emptyList(),
    /** The author's write-up of what solved it; becomes the card's accepted answer. */
    @field:NotBlank
    val solution: String,
) {
    fun toDraft() = PromotionDraft(
        title = title.trim(),
        body = body.trim(),
        environment = environment?.trim()?.takeIf { it.isNotEmpty() },
        logs = logs?.trim()?.takeIf { it.isNotEmpty() },
        tags = tags,
        solution = solution.trim(),
    )
}
