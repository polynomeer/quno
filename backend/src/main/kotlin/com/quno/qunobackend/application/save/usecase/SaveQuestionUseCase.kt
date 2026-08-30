package com.quno.qunobackend.application.save.usecase

import com.quno.qunobackend.domain.question.QuestionNotFoundException
import com.quno.qunobackend.domain.question.QuestionRepository
import com.quno.qunobackend.domain.save.SaveRepository
import org.springframework.stereotype.Service

@Service
class SaveQuestionUseCase(
    private val questionRepository: QuestionRepository,
    private val saveRepository: SaveRepository,
) {
    fun execute(userId: Long, questionId: Long) {
        questionRepository.findById(questionId) ?: throw QuestionNotFoundException(questionId)
        saveRepository.save(userId, questionId)
    }
}
