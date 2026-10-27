package com.quno.qunobackend.interfaces.api.stack

import com.quno.qunobackend.application.stack.usecase.StackWardSuggestionResult
import com.quno.qunobackend.domain.question.QuestionStatus
import com.quno.qunobackend.domain.stack.StackDependency
import com.quno.qunobackend.interfaces.api.question.DependencyRequest
import com.quno.qunobackend.interfaces.api.question.DependencyResponse
import jakarta.validation.Valid
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Size
import java.time.Instant

/** `PUT /me/stack` replaces the whole list; `[]` clears it (ADR-0078). */
data class ReplaceMyStackRequest(
    @field:NotNull @field:Valid @field:Size(max = StackDependency.MAX_PER_QUESTION)
    val dependencies: List<DependencyRequest>,
)

data class MyStackResponse(val dependencies: List<DependencyResponse>)

data class StackWardSuggestionResponse(
    val questionId: Long,
    val title: String,
    val status: QuestionStatus,
    val createdAt: Instant,
    val sharedDependencies: List<DependencyResponse>,
)

fun StackDependency.toResponse() = DependencyResponse(ecosystem, name, version)

fun StackWardSuggestionResult.toResponse() = StackWardSuggestionResponse(
    questionId = questionId,
    title = title,
    status = status,
    createdAt = createdAt,
    sharedDependencies = sharedDependencies.map { it.toResponse() },
)
