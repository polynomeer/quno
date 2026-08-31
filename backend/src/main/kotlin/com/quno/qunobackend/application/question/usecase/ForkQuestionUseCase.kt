package com.quno.qunobackend.application.question.usecase

import com.quno.qunobackend.application.question.dto.ForkQuestionCommand
import com.quno.qunobackend.application.question.dto.QuestionMutationResult
import com.quno.qunobackend.domain.question.Question
import com.quno.qunobackend.domain.question.QuestionNotFoundException
import com.quno.qunobackend.domain.question.QuestionRepository
import com.quno.qunobackend.domain.question.QuestionVersion
import com.quno.qunobackend.domain.question.QuestionVersionRepository
import com.quno.qunobackend.domain.tag.QuestionTagRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * Forks a question: copies its current latest content + tags verbatim into a brand new,
 * independent question authored by the forker (Phase 18, ADR-0030) — mirrors GitHub Fork.
 * Does not join the origin's Cluster (Cluster means "same problem"; Fork means "a variant that
 * may need a different answer"). Editing the fork afterward reuses the existing question
 * revision flow (`POST /questions/{id}/versions`) rather than a bespoke "edit on fork" step.
 */
@Service
class ForkQuestionUseCase(
    private val questionRepository: QuestionRepository,
    private val questionVersionRepository: QuestionVersionRepository,
    private val questionTagRepository: QuestionTagRepository,
) {
    @Transactional
    fun execute(command: ForkQuestionCommand): QuestionMutationResult {
        val origin = questionRepository.findById(command.originQuestionId)
            ?: throw QuestionNotFoundException(command.originQuestionId)
        val originVersion = questionVersionRepository.findById(requireNotNull(origin.latestVersionId))
            ?: throw QuestionNotFoundException(command.originQuestionId)

        val question = questionRepository.save(
            Question.open(authorId = command.actorId, title = originVersion.title, originQuestionId = origin.id),
        )
        val questionId = requireNotNull(question.id)

        val version = questionVersionRepository.save(
            QuestionVersion.create(
                questionId = questionId,
                versionNumber = 1,
                title = originVersion.title,
                bodyMarkdown = originVersion.bodyMarkdown,
                environment = originVersion.environment,
                logs = originVersion.logs,
                createdBy = command.actorId,
            ),
        )

        questionTagRepository.findTagsByQuestionId(command.originQuestionId).forEach { tag ->
            questionTagRepository.attach(questionId, requireNotNull(tag.id))
        }

        val updatedQuestion = questionRepository.save(question.withLatestVersion(requireNotNull(version.id)))

        return QuestionMutationResult(
            id = questionId,
            title = updatedQuestion.title,
            status = updatedQuestion.status,
            versionNumber = version.versionNumber,
        )
    }
}
