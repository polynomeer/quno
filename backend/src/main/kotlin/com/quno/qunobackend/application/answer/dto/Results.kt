package com.quno.qunobackend.application.answer.dto

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
