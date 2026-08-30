package com.quno.qunobackend.application.save.usecase

import com.quno.qunobackend.application.save.dto.SavedQuestionResult
import com.quno.qunobackend.domain.question.QuestionRepository
import com.quno.qunobackend.domain.save.SaveRepository
import org.springframework.stereotype.Service

@Service
class ListMySavesUseCase(
    private val saveRepository: SaveRepository,
    private val questionRepository: QuestionRepository,
) {
    fun execute(userId: Long): List<SavedQuestionResult> =
        saveRepository.findSavedQuestionIds(userId).mapNotNull { questionId ->
            questionRepository.findById(questionId)?.let { question ->
                SavedQuestionResult(questionId = questionId, title = question.title, status = question.status)
            }
        }
}
