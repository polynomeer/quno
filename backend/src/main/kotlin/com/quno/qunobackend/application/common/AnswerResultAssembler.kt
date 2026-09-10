package com.quno.qunobackend.application.common

import com.quno.qunobackend.application.answer.dto.AnswerResult
import com.quno.qunobackend.domain.answer.Answer
import com.quno.qunobackend.domain.question.QuestionRepository
import com.quno.qunobackend.domain.question.QuestionVersionRepository
import com.quno.qunobackend.domain.vote.VoteRepository
import com.quno.qunobackend.domain.vote.VoteTargetType
import org.springframework.stereotype.Component

/**
 * Assembles [AnswerResult] with the "is this answer stale?" flag (PLAN.md 5.1) and vote `score`
 * (PLAN.md 11.3) — an answer is stale once its question has been revised past the version it
 * targeted. Shared because WriteAnswer/ListAnswers/GetUserProfile all need this, and profile
 * answers can span many different questions, so the per-question lookup is cached within one call.
 */
@Component
class AnswerResultAssembler(
    private val questionRepository: QuestionRepository,
    private val questionVersionRepository: QuestionVersionRepository,
    private val voteRepository: VoteRepository,
) {
    fun toResult(answer: Answer): AnswerResult = toResults(listOf(answer)).single()

    /** 투표 점수를 답변 하나씩 조회하던 것을 배치 쿼리로 바꿨다(quality-improvement-plan.md
     * Q-2) — 질문 하나에 답변이 N개면 N번 나가던 쿼리가 결과 개수와 무관하게 1번으로 줄었다. */
    fun toResults(answers: List<Answer>): List<AnswerResult> {
        if (answers.isEmpty()) return emptyList()

        val latestVersionCache = mutableMapOf<Long, Int?>()
        fun latestVersionNumberOf(questionId: Long): Int? = latestVersionCache.getOrPut(questionId) {
            val latestVersionId = questionRepository.findById(questionId)?.latestVersionId ?: return@getOrPut null
            questionVersionRepository.findById(latestVersionId)?.versionNumber
        }

        val scoresByAnswerId = voteRepository.sumScoresByTargets(VoteTargetType.ANSWER, answers.mapNotNull { it.id })

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
                score = scoresByAnswerId[answer.id] ?: 0L,
                createdAt = answer.createdAt,
                updatedAt = answer.updatedAt,
            )
        }
    }
}
