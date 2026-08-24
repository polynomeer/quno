package com.quno.qunobackend.application.question.usecase

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
) {
    fun execute(questionId: Long, versionNumber: Int): QuestionVersionResult {
        questionRepository.findById(questionId) ?: throw QuestionNotFoundException(questionId)
        val version = questionVersionRepository.findByQuestionIdAndVersionNumber(questionId, versionNumber)
            ?: throw QuestionVersionNotFoundException(questionId, versionNumber)

        return QuestionVersionResult(
            questionId = questionId,
            versionNumber = version.versionNumber,
            title = version.title,
            body = version.bodyMarkdown,
            environment = version.environment,
            logs = version.logs,
            createdBy = version.createdBy,
            createdAt = version.createdAt,
        )
    }
}
