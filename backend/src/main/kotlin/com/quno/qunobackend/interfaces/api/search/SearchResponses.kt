package com.quno.qunobackend.interfaces.api.search

import com.quno.qunobackend.application.search.dto.QuestionSearchResult
import com.quno.qunobackend.domain.question.QuestionStatus
import java.time.Instant

data class QuestionSearchResultResponse(
    val id: Long,
    val title: String,
    val status: QuestionStatus,
    val tags: List<String>,
    val score: Long,
    val answerCount: Int,
    val hasAcceptedAnswer: Boolean,
    val versionNumber: Int,
    val createdAt: Instant,
    val updatedAt: Instant,
)

fun QuestionSearchResult.toResponse() = QuestionSearchResultResponse(
    id = id,
    title = title,
    status = status,
    tags = tags,
    score = score,
    answerCount = answerCount,
    hasAcceptedAnswer = hasAcceptedAnswer,
    versionNumber = versionNumber,
    createdAt = createdAt,
    updatedAt = updatedAt,
)
