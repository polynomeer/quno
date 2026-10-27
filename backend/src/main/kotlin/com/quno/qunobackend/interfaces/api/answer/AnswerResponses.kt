package com.quno.qunobackend.interfaces.api.answer

import com.quno.qunobackend.application.answer.dto.AnswerMutationResult
import com.quno.qunobackend.application.answer.dto.AnswerResult
import com.quno.qunobackend.domain.question.DiffLineType
import com.quno.qunobackend.domain.question.QuestionStatus
import com.quno.qunobackend.domain.stack.ApplicabilityRange
import com.quno.qunobackend.domain.stack.Ecosystem
import java.time.Instant

data class AnswerResponse(
    val id: Long,
    val questionId: Long,
    val authorId: Long,
    /** Filled by list endpoints (ADR-0064); null where a response isn't enriched (e.g. create). */
    val authorNickname: String?,
    val body: String,
    val isAccepted: Boolean,
    val targetVersionNumber: Int,
    val isStale: Boolean,
    val score: Long,
    val createdAt: Instant,
    val updatedAt: Instant,
    /** Shown as "질문방 대화에서 정리됨" (ADR-0073); null for ordinary answers. */
    val sourceLiveChatRoomId: Long?,
    /** Where the answer holds, per package — `[fromVersion, untilVersion)` (ADR-0077). */
    val applicability: List<ApplicabilityRangeResponse>,
)

data class ApplicabilityRangeResponse(val ecosystem: Ecosystem, val name: String, val fromVersion: String?, val untilVersion: String?)

fun ApplicabilityRange.toResponse() = ApplicabilityRangeResponse(ecosystem, name, fromVersion, untilVersion)

fun AnswerResult.toResponse(authorNickname: String? = null) = AnswerResponse(
    id = id,
    questionId = questionId,
    authorId = authorId,
    authorNickname = authorNickname,
    body = body,
    isAccepted = isAccepted,
    targetVersionNumber = targetVersionNumber,
    isStale = isStale,
    score = score,
    createdAt = createdAt,
    updatedAt = updatedAt,
    sourceLiveChatRoomId = sourceLiveChatRoomId,
    applicability = applicability.map { it.toResponse() },
)

data class AcceptAnswerResponse(
    val questionId: Long,
    val answerId: Long,
    val questionStatus: QuestionStatus,
)

data class AnswerMutationResponse(
    val id: Long,
    val questionId: Long,
    val versionNumber: Int,
)

fun AnswerMutationResult.toResponse() = AnswerMutationResponse(id = id, questionId = questionId, versionNumber = versionNumber)

data class AnswerVersionResponse(
    val answerId: Long,
    val versionNumber: Int,
    val body: String,
    val createdBy: Long,
    val createdAt: Instant,
)

data class AnswerVersionSummaryResponse(
    val versionNumber: Int,
    val createdBy: Long,
    val createdByNickname: String?,
    val createdAt: Instant,
)

data class AnswerDiffLineResponse(val type: DiffLineType, val text: String)

data class AnswerVersionDiffResponse(
    val fromVersion: Int,
    val toVersion: Int,
    val lines: List<AnswerDiffLineResponse>,
)
