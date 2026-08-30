package com.quno.qunobackend.application.save.usecase

import com.quno.qunobackend.domain.save.SaveRepository
import org.springframework.stereotype.Service

@Service
class UnsaveQuestionUseCase(
    private val saveRepository: SaveRepository,
) {
    fun execute(userId: Long, questionId: Long) {
        saveRepository.unsave(userId, questionId)
    }
}
