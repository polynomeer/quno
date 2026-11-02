package com.quno.qunobackend.application.question.dto

import com.quno.qunobackend.domain.stack.StackDependency

data class CreateQuestionCommand(
    val authorId: Long,
    val title: String,
    val body: String,
    val environment: String?,
    val logs: String?,
    val tagNames: List<String> = emptyList(),
    /** Stack fingerprint entries the author kept from the parse preview (ADR-0076). */
    val dependencies: List<StackDependency> = emptyList(),
    /** A `quno.snapshot` JSON document (ADR-0083); scrubbed and fingerprinted again on the server. */
    val environmentSnapshot: String? = null,
    /** Set to ask one verified organization privately (Phase 79, ADR-0085); null = public. */
    val organizationId: Long? = null,
)

data class ReviseQuestionCommand(
    val questionId: Long,
    val actorId: Long,
    val title: String,
    val body: String,
    val environment: String?,
    val logs: String?,
    /** Null keeps the current stack fingerprint; an empty list clears it (ADR-0076). */
    val dependencies: List<StackDependency>? = null,
    /** Null keeps the current environment snapshot; a document replaces it (ADR-0083). */
    val environmentSnapshot: String? = null,
)

data class MarkQuestionOutdatedCommand(
    val questionId: Long,
    val actorId: Long,
    val reason: String,
)

data class ForkQuestionCommand(
    val originQuestionId: Long,
    val actorId: Long,
)
