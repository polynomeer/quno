package com.quno.qunobackend.application.question.usecase

import com.quno.qunobackend.application.engine.QuestionContentMasker
import com.quno.qunobackend.application.question.dto.QuestionSummaryResult
import com.quno.qunobackend.domain.question.QuestionNotFoundException
import com.quno.qunobackend.domain.question.QuestionRepository
import com.quno.qunobackend.domain.question.QuestionVersionRepository
import com.quno.qunobackend.domain.question.QuestionViewCountRepository
import com.quno.qunobackend.domain.stack.QuestionDependencyRepository
import com.quno.qunobackend.domain.tag.QuestionTagRepository
import com.quno.qunobackend.domain.vote.VoteRepository
import com.quno.qunobackend.domain.vote.VoteTargetType
import org.springframework.stereotype.Service

@Service
class GetQuestionUseCase(
    private val questionRepository: QuestionRepository,
    private val questionVersionRepository: QuestionVersionRepository,
    private val questionTagRepository: QuestionTagRepository,
    private val voteRepository: VoteRepository,
    private val questionViewCountRepository: QuestionViewCountRepository,
    /** Null only in unit tests that don't care about the stack fingerprint; Spring always injects it. */
    private val questionDependencyRepository: QuestionDependencyRepository? = null,
    private val contentMasker: QuestionContentMasker? = null,
) {
    /** [viewerId] null is an anonymous reader: secrets are masked for everyone but the author (ADR-0081). */
    fun execute(questionId: Long, viewerId: Long? = null): QuestionSummaryResult {
        val question = questionRepository.findById(questionId) ?: throw QuestionNotFoundException(questionId)
        val latestVersionId = requireNotNull(question.latestVersionId) {
            "question $questionId has no latest version yet"
        }
        val latestVersion = questionVersionRepository.findById(latestVersionId)
            ?: throw QuestionNotFoundException(questionId)
        val mask = contentMasker?.forViewer(questionId, question.authorId, viewerId) ?: { it }
        val maskedCount = contentMasker?.maskedCount(questionId, question.authorId, viewerId, question.title, latestVersion.bodyMarkdown, latestVersion.environment, latestVersion.logs) ?: 0

        return QuestionSummaryResult(
            id = questionId,
            authorId = question.authorId,
            title = requireNotNull(mask(question.title)),
            status = question.status,
            versionNumber = latestVersion.versionNumber,
            body = requireNotNull(mask(latestVersion.bodyMarkdown)),
            environment = mask(latestVersion.environment),
            logs = mask(latestVersion.logs),
            tags = questionTagRepository.findTagsByQuestionId(questionId).map { it.name },
            score = voteRepository.sumScore(VoteTargetType.QUESTION, questionId),
            viewCount = questionViewCountRepository.countByQuestionId(questionId),
            createdAt = question.createdAt,
            updatedAt = question.updatedAt,
            dependencies = questionDependencyRepository?.findByQuestionId(questionId).orEmpty(),
            maskedSecretCount = maskedCount,
        )
    }
}
