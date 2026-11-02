package com.quno.qunobackend.application.save.usecase

import com.quno.qunobackend.application.save.dto.SavedQuestionResult
import com.quno.qunobackend.domain.question.QuestionRepository
import com.quno.qunobackend.domain.question.QuestionViewer
import com.quno.qunobackend.domain.save.SaveRepository
import org.springframework.stereotype.Service

@Service
class ListMySavesUseCase(
    private val saveRepository: SaveRepository,
    private val questionRepository: QuestionRepository,
) {
    /** [viewer] defaults to "no organizations", which can only hide more, never show more (ADR-0085). */
    fun execute(userId: Long, viewer: QuestionViewer = QuestionViewer(userId, emptySet())): List<SavedQuestionResult> = saveRepository.findSavedQuestionIds(userId).mapNotNull { questionId ->
        // Re-checked on every read: someone who left the organization loses its questions here too.
        questionRepository.findById(questionId)?.takeIf(viewer::canSee)?.let { question ->
            SavedQuestionResult(questionId = questionId, title = question.title, status = question.status)
        }
    }
}
