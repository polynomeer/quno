package com.quno.qunobackend.application.watch.usecase

import com.quno.qunobackend.domain.watch.WatchRepository
import org.springframework.stereotype.Service

@Service
class UnwatchQuestionUseCase(
    private val watchRepository: WatchRepository,
) {
    fun execute(userId: Long, questionId: Long) {
        watchRepository.unwatch(userId, questionId)
    }
}
