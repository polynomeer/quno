package com.quno.qunobackend.application.search.dto

import com.quno.qunobackend.domain.question.QuestionStatus
import java.time.Instant

data class QuestionSearchResult(
    val id: Long,
    val title: String,
    val status: QuestionStatus,
    val tags: List<String>,
    val score: Long,
    /** The Living Question Card's movement signals (ADR-0062) — enough for a list card to show
     * "answered/accepted", "rev N" and "updated X ago" without a per-card detail fetch. */
    val answerCount: Int,
    val hasAcceptedAnswer: Boolean,
    val versionNumber: Int,
    val viewCount: Long,
    val createdAt: Instant,
    val updatedAt: Instant,
)
