package com.quno.qunobackend.application.answer.usecase

import com.quno.qunobackend.application.answer.dto.AnswerResult
import com.quno.qunobackend.domain.answer.AnswerRepository
import com.quno.qunobackend.domain.question.QuestionNotFoundException
import com.quno.qunobackend.domain.question.QuestionRepository
import org.springframework.stereotype.Service

@Service
class ListAnswersUseCase(
    private val questionRepository: QuestionRepository,
    private val answerRepository: AnswerRepository,
) {
    fun execute(questionId: Long): List<AnswerResult> {
        questionRepository.findById(questionId) ?: throw QuestionNotFoundException(questionId)
        return answerRepository.findAllByQuestionId(questionId).map { it.toResult() }
    }
}
