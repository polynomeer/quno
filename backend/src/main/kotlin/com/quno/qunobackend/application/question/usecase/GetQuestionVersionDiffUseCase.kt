package com.quno.qunobackend.application.question.usecase

import com.quno.qunobackend.application.question.dto.QuestionVersionDiffResult
import com.quno.qunobackend.domain.question.QuestionNotFoundException
import com.quno.qunobackend.domain.question.QuestionRepository
import com.quno.qunobackend.domain.question.QuestionVersionNotFoundException
import com.quno.qunobackend.domain.question.QuestionVersionRepository
import com.quno.qunobackend.domain.question.TextDiffer
import org.springframework.stereotype.Service

@Service
class GetQuestionVersionDiffUseCase(
    private val questionRepository: QuestionRepository,
    private val questionVersionRepository: QuestionVersionRepository,
) {
    fun execute(questionId: Long, fromVersion: Int, toVersion: Int): QuestionVersionDiffResult {
        questionRepository.findById(questionId) ?: throw QuestionNotFoundException(questionId)
        val from = questionVersionRepository.findByQuestionIdAndVersionNumber(questionId, fromVersion)
            ?: throw QuestionVersionNotFoundException(questionId, fromVersion)
        val to = questionVersionRepository.findByQuestionIdAndVersionNumber(questionId, toVersion)
            ?: throw QuestionVersionNotFoundException(questionId, toVersion)

        return QuestionVersionDiffResult(
            fromVersion = fromVersion,
            toVersion = toVersion,
            lines = TextDiffer.diffLines(from.bodyMarkdown, to.bodyMarkdown),
        )
    }
}
