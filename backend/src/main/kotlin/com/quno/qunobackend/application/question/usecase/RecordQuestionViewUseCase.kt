package com.quno.qunobackend.application.question.usecase

import com.quno.qunobackend.domain.question.QuestionNotFoundException
import com.quno.qunobackend.domain.question.QuestionRepository
import com.quno.qunobackend.domain.question.QuestionViewCountRepository
import com.quno.qunobackend.domain.question.QuestionViewDeduplicator
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * Counted from an explicit `POST /questions/{id}/views` the detail page sends once on mount — not
 * from `GET /questions/{id}`, which the server-side `generateMetadata` and crawlers also call
 * (ADR-0063).
 */
@Service
class RecordQuestionViewUseCase(
    private val questionRepository: QuestionRepository,
    private val questionViewCountRepository: QuestionViewCountRepository,
    private val questionViewDeduplicator: QuestionViewDeduplicator,
) {
    /** Returns whether this view was counted (false = same viewer within the dedup window). */
    @Transactional
    fun execute(questionId: Long, viewerKey: String): Boolean {
        questionRepository.findById(questionId) ?: throw QuestionNotFoundException(questionId)
        if (!questionViewDeduplicator.isFirstView(questionId, viewerKey)) return false
        questionViewCountRepository.increment(questionId)
        return true
    }
}
