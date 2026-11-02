package com.quno.qunobackend.application.watch.usecase

import com.quno.qunobackend.application.watch.dto.WatchedQuestionResult
import com.quno.qunobackend.domain.question.QuestionRepository
import com.quno.qunobackend.domain.question.QuestionViewer
import com.quno.qunobackend.domain.watch.WatchRepository
import org.springframework.stereotype.Service

@Service
class ListMyWatchesUseCase(
    private val watchRepository: WatchRepository,
    private val questionRepository: QuestionRepository,
) {
    /** [viewer] defaults to "no organizations", which can only hide more, never show more (ADR-0085). */
    fun execute(userId: Long, viewer: QuestionViewer = QuestionViewer(userId, emptySet())): List<WatchedQuestionResult> = watchRepository.findWatchedQuestionIds(userId).mapNotNull { questionId ->
        questionRepository.findById(questionId)?.takeIf(viewer::canSee)?.let { question ->
            WatchedQuestionResult(questionId = questionId, title = question.title, status = question.status)
        }
    }
}
