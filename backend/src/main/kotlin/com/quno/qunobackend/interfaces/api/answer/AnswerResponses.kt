package com.quno.qunobackend.interfaces.api.answer

import com.quno.qunobackend.domain.question.QuestionStatus
import java.time.Instant

data class AnswerResponse(
    val id: Long,
    val questionId: Long,
    val authorId: Long,
    val body: String,
    val isAccepted: Boolean,
    val createdAt: Instant,
    val updatedAt: Instant,
)

data class AcceptAnswerResponse(
    val questionId: Long,
    val answerId: Long,
    val questionStatus: QuestionStatus,
)
