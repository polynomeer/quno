package com.quno.qunobackend.application.question.usecase

import com.quno.qunobackend.application.common.QuestionSummaryHydrator
import com.quno.qunobackend.application.search.dto.QuestionSearchResult
import com.quno.qunobackend.domain.question.QuestionNotFoundException
import com.quno.qunobackend.domain.question.QuestionRepository
import org.springframework.stereotype.Service

@Service
class ListQuestionForksUseCase(
    private val questionRepository: QuestionRepository,
    private val hydrator: QuestionSummaryHydrator,
) {
    fun execute(questionId: Long): List<QuestionSearchResult> {
        questionRepository.findById(questionId) ?: throw QuestionNotFoundException(questionId)
        val forkIds = questionRepository.findAllByOriginQuestionId(questionId).mapNotNull { it.id }
        return hydrator.hydrate(forkIds)
    }
}
