package com.quno.qunobackend.application.answer.dto

import com.quno.qunobackend.domain.question.DiffLine
import com.quno.qunobackend.domain.question.QuestionStatus
import java.time.Instant

data class AnswerResult(
    val id: Long,
    val questionId: Long,
    val authorId: Long,
    val body: String,
    val isAccepted: Boolean,
    val targetVersionNumber: Int,
    /** True when the question has been revised since this answer targeted [targetVersionNumber]. */
    val isStale: Boolean,
    val score: Long,
    val createdAt: Instant,
    val updatedAt: Instant,
)

data class AcceptAnswerResult(
    val questionId: Long,
    val answerId: Long,
    val questionStatus: QuestionStatus,
)

/** Shared shape for both "write" and "revise" (Phase 17) — both just wire an answer to a new version. */
data class AnswerMutationResult(
    val id: Long,
    val questionId: Long,
    val versionNumber: Int,
)

data class AnswerVersionResult(
    val answerId: Long,
    val versionNumber: Int,
    val body: String,
    val createdBy: Long,
    val createdAt: Instant,
)

data class AnswerVersionSummaryResult(
    val versionNumber: Int,
    val createdBy: Long,
    val createdAt: Instant,
)

data class AnswerVersionDiffResult(
    val fromVersion: Int,
    val toVersion: Int,
    val lines: List<DiffLine>,
)
