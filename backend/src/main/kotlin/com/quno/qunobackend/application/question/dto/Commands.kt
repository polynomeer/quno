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
