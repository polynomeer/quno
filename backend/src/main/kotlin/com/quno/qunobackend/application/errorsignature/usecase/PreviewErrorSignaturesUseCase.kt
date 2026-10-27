package com.quno.qunobackend.application.errorsignature.usecase

import com.quno.qunobackend.application.common.QuestionSummaryHydrator
import com.quno.qunobackend.application.errorsignature.dto.ErrorSignaturePreviewResult
import com.quno.qunobackend.domain.errorsignature.ErrorSignatureExtractor
import com.quno.qunobackend.domain.errorsignature.QuestionErrorSignatureRepository
import org.springframework.stereotype.Service

/**
 * "같은 에러 N건" before a question is posted (Phase 64, ADR-0075). Read-only: nothing about the
 * pasted text is stored. Search and Related use the same repository lookup, so all three agree.
 */
@Service
class PreviewErrorSignaturesUseCase(
    private val questionErrorSignatureRepository: QuestionErrorSignatureRepository,
    private val hydrator: QuestionSummaryHydrator,
) {
    fun execute(text: String, limit: Int = 10): ErrorSignaturePreviewResult {
        val signatures = ErrorSignatureExtractor.extractAll(text)
        val questionIds = questionErrorSignatureRepository.findQuestionIdsMatching(signatures, excludeQuestionId = null, limit = limit)
        return ErrorSignaturePreviewResult(signatures = signatures, questions = hydrator.hydrate(questionIds))
    }
}
