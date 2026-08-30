package com.quno.qunobackend.application.question.dto

import com.quno.qunobackend.domain.question.DiffLine
import com.quno.qunobackend.domain.question.QuestionStatus
import java.time.Instant

/** Shared shape for both "create" and "revise", since both just wire a question to a new version. */
data class QuestionMutationResult(
    val id: Long,
    val title: String,
    val status: QuestionStatus,
    val versionNumber: Int,
)

data class QuestionSummaryResult(
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

data class QuestionVersionResult(
    val questionId: Long,
    val versionNumber: Int,
    val title: String,
    val body: String,
    val environment: String?,
    val logs: String?,
    val createdBy: Long,
    val createdAt: Instant,
)

data class QuestionVersionSummaryResult(
    val versionNumber: Int,
    val title: String,
    val createdBy: Long,
    val createdAt: Instant,
)

data class QuestionVersionDiffResult(
    val fromVersion: Int,
    val toVersion: Int,
    val lines: List<DiffLine>,
)
