package com.quno.qunobackend.application.common

import com.quno.qunobackend.application.answer.dto.AnswerResult
import com.quno.qunobackend.domain.answer.Answer
import com.quno.qunobackend.domain.question.QuestionRepository
import com.quno.qunobackend.domain.question.QuestionVersionRepository
import org.springframework.stereotype.Component

/**
 * Assembles [AnswerResult] with the "is this answer stale?" flag (PLAN.md 5.1) — an answer
 * is stale once its question has been revised past the version it targeted. Shared because
 * WriteAnswer/ListAnswers/GetUserProfile all need this, and profile answers can span many
 * different questions, so the per-question lookup is cached within one call.
 */
@Component
class AnswerResultAssembler(
    private val questionRepository: QuestionRepository,
    private val questionVersionRepository: QuestionVersionRepository,
) {
    fun toResult(answer: Answer): AnswerResult = toResults(listOf(answer)).single()

    fun toResults(answers: List<Answer>): List<AnswerResult> {
        val latestVersionCache = mutableMapOf<Long, Int?>()
        fun latestVersionNumberOf(questionId: Long): Int? = latestVersionCache.getOrPut(questionId) {
            val latestVersionId = questionRepository.findById(questionId)?.latestVersionId ?: return@getOrPut null
            questionVersionRepository.findById(latestVersionId)?.versionNumber
        }

        return answers.map { answer ->
            val latestVersionNumber = latestVersionNumberOf(answer.questionId)
            AnswerResult(
                id = requireNotNull(answer.id),
                questionId = answer.questionId,
                authorId = answer.authorId,
                body = answer.bodyMarkdown,
                isAccepted = answer.isAccepted,
                targetVersionNumber = answer.targetVersionNumber,
                isStale = latestVersionNumber != null && answer.targetVersionNumber < latestVersionNumber,
                createdAt = answer.createdAt,
                updatedAt = answer.updatedAt,
            )
        }
    }
}
