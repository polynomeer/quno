package com.quno.qunobackend.application.errorsignature.dto

import com.quno.qunobackend.application.search.dto.QuestionSearchResult
import com.quno.qunobackend.domain.errorsignature.ErrorSignature

/** What the ask form shows while someone pastes a trace: the signatures found and the questions
 * that already share them, strong matches first (Phase 64, ADR-0075). */
data class ErrorSignaturePreviewResult(
    val signatures: List<ErrorSignature>,
    val questions: List<QuestionSearchResult>,
)
