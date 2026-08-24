package com.quno.qunobackend.application.answer.dto

import com.quno.qunobackend.domain.question.QuestionStatus
import java.time.Instant

data class AnswerResult(
    val id: Long,
    val questionId: Long,
    val authorId: Long,
    val body: String,
    val isAccepted: Boolean,
    val createdAt: Instant,
    val updatedAt: Instant,
)

data class AcceptAnswerResult(
    val questionId: Long,
    val answerId: Long,
    val questionStatus: QuestionStatus,
)
