package com.quno.qunobackend.interfaces.api.errorsignature

import com.quno.qunobackend.application.errorsignature.usecase.PreviewErrorSignaturesUseCase
import com.quno.qunobackend.domain.errorsignature.ErrorSignatureExtractor
import com.quno.qunobackend.interfaces.api.search.toResponse
import jakarta.validation.Valid
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController

data class ErrorSignaturePreviewRequest(
    @field:NotBlank
    @field:Size(max = ErrorSignatureExtractor.MAX_INPUT_LENGTH)
    val text: String,
)

/**
 * "같은 에러 N건" for the ask form (Phase 64, ADR-0075). POST because a stack trace doesn't belong
 * in a URL (length, and it may carry paths or host names); nothing in the body is stored.
 */
@RestController
class ErrorSignatureController(
    private val previewErrorSignaturesUseCase: PreviewErrorSignaturesUseCase,
) {
    @PostMapping("/api/v1/error-signatures/preview")
    fun preview(@Valid @RequestBody request: ErrorSignaturePreviewRequest): ErrorSignaturePreviewResponse = previewErrorSignaturesUseCase.execute(request.text).let { result ->
        ErrorSignaturePreviewResponse(
            signatures = result.signatures.map {
                ErrorSignatureResponse(
                    exceptionClass = it.exceptionClass,
                    normalizedMessage = it.normalizedMessage,
                    frames = it.frames,
                    signatureHash = it.signatureHash,
                    messageKey = it.messageKey,
                )
            },
            questions = result.questions.map { it.toResponse() },
        )
    }
}
