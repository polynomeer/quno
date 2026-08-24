package com.quno.qunobackend.interfaces.api.question

import com.quno.qunobackend.domain.question.QuestionStatus
import java.time.Instant

data class CreateQuestionResponse(
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
    val createdAt: Instant,
    val updatedAt: Instant,
)
