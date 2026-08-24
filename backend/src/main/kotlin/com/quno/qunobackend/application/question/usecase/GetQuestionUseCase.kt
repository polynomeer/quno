package com.quno.qunobackend.application.question.usecase

import com.quno.qunobackend.application.question.dto.QuestionSummaryResult
import com.quno.qunobackend.domain.question.QuestionNotFoundException
import com.quno.qunobackend.domain.question.QuestionRepository
import com.quno.qunobackend.domain.question.QuestionVersionRepository
import com.quno.qunobackend.domain.tag.QuestionTagRepository
import org.springframework.stereotype.Service

@Service
class GetQuestionUseCase(
    private val questionRepository: QuestionRepository,
    private val questionVersionRepository: QuestionVersionRepository,
    private val questionTagRepository: QuestionTagRepository,
) {
    fun execute(questionId: Long): QuestionSummaryResult {
        val question = questionRepository.findById(questionId) ?: throw QuestionNotFoundException(questionId)
        val latestVersionId = requireNotNull(question.latestVersionId) {
            "question $questionId has no latest version yet"
        }
        val latestVersion = questionVersionRepository.findById(latestVersionId)
            ?: throw QuestionNotFoundException(questionId)

        return QuestionSummaryResult(
            id = questionId,
            authorId = question.authorId,
            title = question.title,
            status = question.status,
            versionNumber = latestVersion.versionNumber,
            body = latestVersion.bodyMarkdown,
            environment = latestVersion.environment,
            logs = latestVersion.logs,
            tags = questionTagRepository.findTagsByQuestionId(questionId).map { it.name },
            createdAt = question.createdAt,
            updatedAt = question.updatedAt,
        )
    }
}
