package com.quno.qunobackend.interfaces.api.search

import com.quno.qunobackend.application.search.dto.QuestionSearchResult
import com.quno.qunobackend.domain.question.QuestionStatus
import com.quno.qunobackend.domain.question.QuestionVisibility
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
    val viewCount: Long,
    val createdAt: Instant,
    val updatedAt: Instant,
    /** Lets a list card show the lock (Phase 79, ADR-0085). */
    val visibility: QuestionVisibility = QuestionVisibility.PUBLIC,
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
    viewCount = viewCount,
    createdAt = createdAt,
    updatedAt = updatedAt,
    visibility = visibility,
)
