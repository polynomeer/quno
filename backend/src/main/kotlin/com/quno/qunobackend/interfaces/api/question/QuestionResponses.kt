package com.quno.qunobackend.interfaces.api.question

import com.quno.qunobackend.domain.question.DiffLineType
import com.quno.qunobackend.domain.question.QuestionStatus
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
