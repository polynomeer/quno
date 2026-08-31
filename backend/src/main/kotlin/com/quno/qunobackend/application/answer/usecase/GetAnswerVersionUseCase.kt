package com.quno.qunobackend.application.answer.usecase

import com.quno.qunobackend.application.answer.dto.AnswerVersionResult
import com.quno.qunobackend.domain.answer.AnswerNotFoundException
import com.quno.qunobackend.domain.answer.AnswerRepository
import com.quno.qunobackend.domain.answer.AnswerVersionNotFoundException
import com.quno.qunobackend.domain.answer.AnswerVersionRepository
import org.springframework.stereotype.Service

@Service
class GetAnswerVersionUseCase(
    private val answerRepository: AnswerRepository,
    private val answerVersionRepository: AnswerVersionRepository,
) {
    fun execute(answerId: Long, versionNumber: Int): AnswerVersionResult {
        answerRepository.findById(answerId) ?: throw AnswerNotFoundException(answerId)
        val version = answerVersionRepository.findByAnswerIdAndVersionNumber(answerId, versionNumber)
            ?: throw AnswerVersionNotFoundException(answerId, versionNumber)

        return AnswerVersionResult(
            answerId = answerId,
            versionNumber = version.versionNumber,
            body = version.bodyMarkdown,
            createdBy = version.createdBy,
            createdAt = version.createdAt,
        )
    }
}
