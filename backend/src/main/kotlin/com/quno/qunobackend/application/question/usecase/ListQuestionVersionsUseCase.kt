package com.quno.qunobackend.application.question.usecase

import com.quno.qunobackend.application.engine.QuestionContentMasker
import com.quno.qunobackend.application.question.dto.QuestionVersionSummaryResult
import com.quno.qunobackend.domain.question.QuestionNotFoundException
import com.quno.qunobackend.domain.question.QuestionRepository
import com.quno.qunobackend.domain.question.QuestionVersionRepository
import org.springframework.stereotype.Service

@Service
class ListQuestionVersionsUseCase(
    private val questionRepository: QuestionRepository,
    private val questionVersionRepository: QuestionVersionRepository,
    private val contentMasker: QuestionContentMasker? = null,
) {
    fun execute(questionId: Long, viewerId: Long? = null): List<QuestionVersionSummaryResult> {
        val question = questionRepository.findById(questionId) ?: throw QuestionNotFoundException(questionId)
        val mask = contentMasker?.forViewer(questionId, question.authorId, viewerId) ?: { it }

        return questionVersionRepository.findAllByQuestionIdOrderByVersionNumberAsc(questionId).map {
            QuestionVersionSummaryResult(
                versionNumber = it.versionNumber,
                title = requireNotNull(mask(it.title)),
                createdBy = it.createdBy,
                createdAt = it.createdAt,
            )
        }
    }
}
