package com.quno.qunobackend.application.errorsignature

import com.quno.qunobackend.domain.errorsignature.ErrorSignatureExtractor
import com.quno.qunobackend.domain.errorsignature.QuestionErrorSignatureRepository
import org.springframework.stereotype.Component

/**
 * Re-indexes a question's error signatures from its latest version (Phase 64, ADR-0075). Logs come
 * first because that's where traces usually are; the body is included for people who paste the
 * trace into a code block instead.
 */
@Component
class QuestionErrorSignatureIndexer(
    private val questionErrorSignatureRepository: QuestionErrorSignatureRepository,
) {
    fun index(questionId: Long, versionNumber: Int, body: String, logs: String?) {
        val signatures = ErrorSignatureExtractor.extractAll(listOfNotNull(logs, body).joinToString("\n"))
        questionErrorSignatureRepository.replaceForQuestion(questionId, versionNumber, signatures)
    }
}
