package com.quno.qunobackend.application.answer.usecase

import com.quno.qunobackend.application.answer.dto.AnswerVersionSummaryResult
import com.quno.qunobackend.domain.answer.AnswerNotFoundException
import com.quno.qunobackend.domain.answer.AnswerRepository
import com.quno.qunobackend.domain.answer.AnswerVersionRepository
import org.springframework.stereotype.Service

@Service
class ListAnswerVersionsUseCase(
    private val answerRepository: AnswerRepository,
    private val answerVersionRepository: AnswerVersionRepository,
) {
    fun execute(answerId: Long): List<AnswerVersionSummaryResult> {
        answerRepository.findById(answerId) ?: throw AnswerNotFoundException(answerId)

        return answerVersionRepository.findAllByAnswerIdOrderByVersionNumberAsc(answerId).map {
            AnswerVersionSummaryResult(versionNumber = it.versionNumber, createdBy = it.createdBy, createdAt = it.createdAt)
        }
    }
}
