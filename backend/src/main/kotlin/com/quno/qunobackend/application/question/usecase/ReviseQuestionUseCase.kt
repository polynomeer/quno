package com.quno.qunobackend.application.question.usecase

import com.quno.qunobackend.application.question.dto.QuestionMutationResult
import com.quno.qunobackend.application.question.dto.ReviseQuestionCommand
import com.quno.qunobackend.domain.question.QuestionAccessDeniedException
import com.quno.qunobackend.domain.question.QuestionNotFoundException
import com.quno.qunobackend.domain.question.QuestionRepository
import com.quno.qunobackend.domain.question.QuestionVersion
import com.quno.qunobackend.domain.question.QuestionVersionRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * Appends a new QuestionVersion (Qv2+). Locks the question row (SELECT ... FOR UPDATE via
 * QuestionRepository#findByIdForUpdate) so two concurrent revisions can't compute the same
 * next version number — see docs/architecture/domain-model.md#revision-생성-동시성-주의.
 */
@Service
class ReviseQuestionUseCase(
    private val questionRepository: QuestionRepository,
    private val questionVersionRepository: QuestionVersionRepository,
) {
    @Transactional
    fun execute(command: ReviseQuestionCommand): QuestionMutationResult {
        val question = questionRepository.findByIdForUpdate(command.questionId)
            ?: throw QuestionNotFoundException(command.questionId)
        if (question.authorId != command.actorId) throw QuestionAccessDeniedException(command.questionId)

        val latestVersionId = requireNotNull(question.latestVersionId) {
            "question ${command.questionId} has no latest version yet"
        }
        val latestVersion = questionVersionRepository.findById(latestVersionId)
            ?: throw QuestionNotFoundException(command.questionId)

        val newVersion = questionVersionRepository.save(
            QuestionVersion.create(
                questionId = command.questionId,
                versionNumber = latestVersion.versionNumber + 1,
                title = command.title,
                bodyMarkdown = command.body,
                environment = command.environment,
                logs = command.logs,
                createdBy = command.actorId,
            ),
        )

        val updatedQuestion = questionRepository.save(
            question.revise(versionId = requireNotNull(newVersion.id), title = command.title),
        )

        return QuestionMutationResult(
            id = command.questionId,
            title = updatedQuestion.title,
            status = updatedQuestion.status,
            versionNumber = newVersion.versionNumber,
        )
    }
}
