package com.quno.qunobackend.interfaces.api.question

import com.quno.qunobackend.application.cluster.dto.QuestionGraphResult
import com.quno.qunobackend.domain.question.DiffLineType
import com.quno.qunobackend.domain.question.QuestionStatus
import com.quno.qunobackend.interfaces.api.search.QuestionSearchResultResponse
import com.quno.qunobackend.interfaces.api.search.toResponse
import java.time.Instant

/** Shared shape for both "create" and "revise" responses. */
data class QuestionMutationResponse(
    val id: Long,
    val title: String,
    val status: QuestionStatus,
    val versionNumber: Int,
)

data class QuestionResponse(
    val id: Long,
    val authorId: Long,
    val title: String,
    val status: QuestionStatus,
    val versionNumber: Int,
    val body: String,
    val environment: String?,
    val logs: String?,
    val tags: List<String>,
    val score: Long,
    val createdAt: Instant,
    val updatedAt: Instant,
)

data class QuestionVersionResponse(
    val questionId: Long,
    val versionNumber: Int,
    val title: String,
    val body: String,
    val environment: String?,
    val logs: String?,
    val createdBy: Long,
    val createdAt: Instant,
)

data class QuestionVersionSummaryResponse(
    val versionNumber: Int,
    val title: String,
    val createdBy: Long,
    val createdAt: Instant,
)

data class DiffLineResponse(val type: DiffLineType, val text: String)

data class QuestionVersionDiffResponse(
    val fromVersion: Int,
    val toVersion: Int,
    val lines: List<DiffLineResponse>,
)

data class QuestionGraphResponse(
    val questionId: Long,
    val clusterMembers: List<QuestionSearchResultResponse>,
    val forkedFrom: QuestionSearchResultResponse?,
    val forks: List<QuestionSearchResultResponse>,
    val relatedQuestions: List<QuestionSearchResultResponse>,
)

fun QuestionGraphResult.toResponse() = QuestionGraphResponse(
    questionId = questionId,
    clusterMembers = clusterMembers.map { it.toResponse() },
    forkedFrom = forkedFrom?.toResponse(),
    forks = forks.map { it.toResponse() },
    relatedQuestions = relatedQuestions.map { it.toResponse() },
)
