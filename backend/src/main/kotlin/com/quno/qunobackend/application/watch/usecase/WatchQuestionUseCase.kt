package com.quno.qunobackend.application.watch.usecase

import com.quno.qunobackend.domain.question.QuestionNotFoundException
import com.quno.qunobackend.domain.question.QuestionRepository
import com.quno.qunobackend.domain.watch.WatchRepository
import org.springframework.stereotype.Service

@Service
class WatchQuestionUseCase(
    private val questionRepository: QuestionRepository,
    private val watchRepository: WatchRepository,
) {
    fun execute(userId: Long, questionId: Long) {
        questionRepository.findById(questionId) ?: throw QuestionNotFoundException(questionId)
        watchRepository.watch(userId, questionId)
    }
}
