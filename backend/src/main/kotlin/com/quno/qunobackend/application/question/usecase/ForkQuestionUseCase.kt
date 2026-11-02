package com.quno.qunobackend.application.question.usecase

import com.quno.qunobackend.application.engine.QuestionContentMasker
import com.quno.qunobackend.application.engine.QuestionEngine
import com.quno.qunobackend.application.errorsignature.QuestionErrorSignatureIndexer
import com.quno.qunobackend.application.question.dto.ForkQuestionCommand
import com.quno.qunobackend.application.question.dto.QuestionMutationResult
import com.quno.qunobackend.domain.engine.QuestionContent
import com.quno.qunobackend.domain.question.Question
import com.quno.qunobackend.domain.question.QuestionNotFoundException
import com.quno.qunobackend.domain.question.QuestionRepository
import com.quno.qunobackend.domain.question.QuestionVersion
import com.quno.qunobackend.domain.question.QuestionVersionRepository
import com.quno.qunobackend.domain.stack.QuestionDependencyRepository
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

    /** Null only in unit tests that don't care about signatures; Spring always injects it. */
    private val errorSignatureIndexer: QuestionErrorSignatureIndexer? = null,
    private val questionDependencyRepository: QuestionDependencyRepository? = null,
    private val questionEngine: QuestionEngine? = null,
    private val contentMasker: QuestionContentMasker? = null,
) {
    @Transactional
    fun execute(command: ForkQuestionCommand): QuestionMutationResult {
        val origin = questionRepository.findById(command.originQuestionId)
            ?: throw QuestionNotFoundException(command.originQuestionId)
        val originVersion = questionVersionRepository.findById(requireNotNull(origin.latestVersionId))
            ?: throw QuestionNotFoundException(command.originQuestionId)

        // A fork is someone else's copy: it starts from what they could see, secrets masked (ADR-0081).
        val mask = contentMasker?.forViewer(command.originQuestionId, origin.authorId, command.actorId) ?: { it }
        val content = QuestionContent(
            title = requireNotNull(mask(originVersion.title)),
            body = requireNotNull(mask(originVersion.bodyMarkdown)),
            environment = mask(originVersion.environment),
            logs = mask(originVersion.logs),
        )
        val question = questionRepository.save(
            // A fork copies the origin's content, so it keeps the origin's scope (ADR-0085): forking
            // never turns a private question public.
            Question.open(
                authorId = command.actorId,
                title = content.title,
                originQuestionId = origin.id,
                visibility = origin.visibility,
                organizationId = origin.organizationId,
            ),
        )
        val questionId = requireNotNull(question.id)

        val version = questionVersionRepository.save(
            QuestionVersion.create(
                questionId = questionId,
                versionNumber = 1,
                title = content.title,
                bodyMarkdown = content.body,
                environment = content.environment,
                logs = content.logs,
                createdBy = command.actorId,
            ),
        )

        questionTagRepository.findTagsByQuestionId(command.originQuestionId).forEach { tag ->
            questionTagRepository.attach(questionId, requireNotNull(tag.id))
        }

        errorSignatureIndexer?.index(questionId, version.versionNumber, content.body, content.logs)
        questionEngine?.analyze(questionId, command.actorId, version.versionNumber, content)
        // A fork starts from the same environment as its origin (ADR-0076).
        questionDependencyRepository?.let { repository ->
            val originDependencies = repository.findByQuestionId(command.originQuestionId)
            if (originDependencies.isNotEmpty()) repository.replaceForQuestion(questionId, version.versionNumber, originDependencies)
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
