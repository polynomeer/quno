package com.quno.qunobackend.application.question.usecase

import com.quno.qunobackend.application.engine.QuestionContentMasker
import com.quno.qunobackend.application.question.dto.QuestionVersionResult
import com.quno.qunobackend.domain.question.QuestionNotFoundException
import com.quno.qunobackend.domain.question.QuestionRepository
import com.quno.qunobackend.domain.question.QuestionVersionNotFoundException
import com.quno.qunobackend.domain.question.QuestionVersionRepository
import org.springframework.stereotype.Service

@Service
class GetQuestionVersionUseCase(
    private val questionRepository: QuestionRepository,
    private val questionVersionRepository: QuestionVersionRepository,
    private val contentMasker: QuestionContentMasker? = null,
) {
    /** Old versions keep whatever was pasted, so they are masked like the latest one (ADR-0081). */
    fun execute(questionId: Long, versionNumber: Int, viewerId: Long? = null): QuestionVersionResult {
        val question = questionRepository.findById(questionId) ?: throw QuestionNotFoundException(questionId)
        val mask = contentMasker?.forViewer(questionId, question.authorId, viewerId) ?: { it }
        val version = questionVersionRepository.findByQuestionIdAndVersionNumber(questionId, versionNumber)
            ?: throw QuestionVersionNotFoundException(questionId, versionNumber)

        return QuestionVersionResult(
            questionId = questionId,
            versionNumber = version.versionNumber,
            title = requireNotNull(mask(version.title)),
            body = requireNotNull(mask(version.bodyMarkdown)),
            environment = mask(version.environment),
            logs = mask(version.logs),
            createdBy = version.createdBy,
            createdAt = version.createdAt,
        )
    }
}
