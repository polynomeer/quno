package com.quno.qunobackend.application.answer.usecase

import com.quno.qunobackend.application.answer.dto.AnswerVersionDiffResult
import com.quno.qunobackend.domain.answer.AnswerNotFoundException
import com.quno.qunobackend.domain.answer.AnswerRepository
import com.quno.qunobackend.domain.answer.AnswerVersionNotFoundException
import com.quno.qunobackend.domain.answer.AnswerVersionRepository
import com.quno.qunobackend.domain.question.TextDiffer
import org.springframework.stereotype.Service

/** Reuses TextDiffer as-is — it's a pure two-string diff, not Question-specific (ADR-0029). */
@Service
class GetAnswerVersionDiffUseCase(
    private val answerRepository: AnswerRepository,
    private val answerVersionRepository: AnswerVersionRepository,
) {
    fun execute(answerId: Long, fromVersion: Int, toVersion: Int): AnswerVersionDiffResult {
        answerRepository.findById(answerId) ?: throw AnswerNotFoundException(answerId)
        val from = answerVersionRepository.findByAnswerIdAndVersionNumber(answerId, fromVersion)
            ?: throw AnswerVersionNotFoundException(answerId, fromVersion)
        val to = answerVersionRepository.findByAnswerIdAndVersionNumber(answerId, toVersion)
            ?: throw AnswerVersionNotFoundException(answerId, toVersion)

        return AnswerVersionDiffResult(
            fromVersion = fromVersion,
            toVersion = toVersion,
            lines = TextDiffer.diffLines(from.bodyMarkdown, to.bodyMarkdown),
        )
    }
}
