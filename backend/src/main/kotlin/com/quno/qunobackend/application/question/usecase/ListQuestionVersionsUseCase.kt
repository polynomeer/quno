package com.quno.qunobackend.application.question.usecase

import com.quno.qunobackend.application.question.dto.QuestionVersionSummaryResult
import com.quno.qunobackend.domain.question.QuestionNotFoundException
import com.quno.qunobackend.domain.question.QuestionRepository
import com.quno.qunobackend.domain.question.QuestionVersionRepository
import org.springframework.stereotype.Service

@Service
class ListQuestionVersionsUseCase(
    private val questionRepository: QuestionRepository,
    private val questionVersionRepository: QuestionVersionRepository,
) {
    fun execute(questionId: Long): List<QuestionVersionSummaryResult> {
        questionRepository.findById(questionId) ?: throw QuestionNotFoundException(questionId)

        return questionVersionRepository.findAllByQuestionIdOrderByVersionNumberAsc(questionId).map {
            QuestionVersionSummaryResult(
                versionNumber = it.versionNumber,
                title = it.title,
                createdBy = it.createdBy,
                createdAt = it.createdAt,
            )
        }
    }
}
