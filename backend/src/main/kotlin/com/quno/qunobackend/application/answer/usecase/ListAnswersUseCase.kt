package com.quno.qunobackend.application.answer.usecase

import com.quno.qunobackend.application.answer.dto.AnswerResult
import com.quno.qunobackend.application.common.AnswerResultAssembler
import com.quno.qunobackend.domain.answer.AnswerRepository
import com.quno.qunobackend.domain.question.QuestionNotFoundException
import com.quno.qunobackend.domain.question.QuestionRepository
import org.springframework.stereotype.Service

@Service
class ListAnswersUseCase(
    private val questionRepository: QuestionRepository,
    private val answerRepository: AnswerRepository,
    private val answerResultAssembler: AnswerResultAssembler,
) {
    fun execute(questionId: Long): List<AnswerResult> {
        questionRepository.findById(questionId) ?: throw QuestionNotFoundException(questionId)
        return answerResultAssembler.toResults(answerRepository.findAllByQuestionId(questionId))
    }
}
