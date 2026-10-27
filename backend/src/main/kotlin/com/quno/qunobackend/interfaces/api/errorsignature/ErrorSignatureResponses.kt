package com.quno.qunobackend.interfaces.api.errorsignature

import com.quno.qunobackend.interfaces.api.search.QuestionSearchResultResponse

data class ErrorSignatureResponse(
    val exceptionClass: String,
    val normalizedMessage: String,
    val frames: List<String>,
    val signatureHash: String?,
    val messageKey: String,
)

data class ErrorSignaturePreviewResponse(
    val signatures: List<ErrorSignatureResponse>,
    /** Questions with the same error, signature-hash matches before message-only ones. */
    val questions: List<QuestionSearchResultResponse>,
)
