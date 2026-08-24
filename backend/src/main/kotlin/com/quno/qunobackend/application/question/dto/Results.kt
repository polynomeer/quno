package com.quno.qunobackend.application.question.dto

import com.quno.qunobackend.domain.question.QuestionStatus
import java.time.Instant

data class CreateQuestionResult(
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
    val createdAt: Instant,
    val updatedAt: Instant,
)
