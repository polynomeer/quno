package com.quno.qunobackend.application.watch.usecase

import com.quno.qunobackend.application.watch.dto.WatchedQuestionResult
import com.quno.qunobackend.domain.question.QuestionRepository
import com.quno.qunobackend.domain.watch.WatchRepository
import org.springframework.stereotype.Service

@Service
class ListMyWatchesUseCase(
    private val watchRepository: WatchRepository,
    private val questionRepository: QuestionRepository,
) {
    fun execute(userId: Long): List<WatchedQuestionResult> =
        watchRepository.findWatchedQuestionIds(userId).mapNotNull { questionId ->
            questionRepository.findById(questionId)?.let { question ->
                WatchedQuestionResult(questionId = questionId, title = question.title, status = question.status)
            }
        }
}
