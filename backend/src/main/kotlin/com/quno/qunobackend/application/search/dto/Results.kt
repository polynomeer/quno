package com.quno.qunobackend.application.search.dto

import com.quno.qunobackend.domain.question.QuestionStatus
import com.quno.qunobackend.domain.question.QuestionVisibility
import com.quno.qunobackend.domain.verification.VerificationLevel
import java.time.Instant

data class QuestionSearchResult(
    val id: Long,
    val title: String,
    val status: QuestionStatus,
    /** So a list card can show the lock (Phase 79, ADR-0085). */
    val visibility: QuestionVisibility = QuestionVisibility.PUBLIC,
    val organizationId: Long? = null,
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
    /** L2 or above with a fresh reproduction (Phase 94, ADR-0089); null otherwise. */
    val verifiedLevel: VerificationLevel? = null,
)
