package com.quno.qunobackend.application.question.usecase

import com.quno.qunobackend.application.question.dto.CreateQuestionCommand
import com.quno.qunobackend.application.question.dto.QuestionMutationResult
import com.quno.qunobackend.domain.question.Question
import com.quno.qunobackend.domain.question.QuestionRepository
import com.quno.qunobackend.domain.question.QuestionVersion
import com.quno.qunobackend.domain.question.QuestionVersionRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * Creates a Question together with its first QuestionVersion (Qv1) in one transaction,
 * following the two-insert-then-update flow in docs/architecture/domain-model.md
 * (the questions/question_versions FK cycle requires the question row to exist first).
 */
@Service
class CreateQuestionUseCase(
    private val questionRepository: QuestionRepository,
    private val questionVersionRepository: QuestionVersionRepository,
) {
    @Transactional
    fun execute(command: CreateQuestionCommand): QuestionMutationResult {
        val question = questionRepository.save(Question.open(authorId = command.authorId, title = command.title))
        val questionId = requireNotNull(question.id)

        val version = questionVersionRepository.save(
            QuestionVersion.create(
                questionId = questionId,
                versionNumber = 1,
                title = command.title,
                bodyMarkdown = command.body,
                environment = command.environment,
                logs = command.logs,
                createdBy = command.authorId,
            ),
        )

        val updatedQuestion = questionRepository.save(question.withLatestVersion(requireNotNull(version.id)))

        return QuestionMutationResult(
            id = questionId,
            title = updatedQuestion.title,
            status = updatedQuestion.status,
            versionNumber = version.versionNumber,
        )
    }
}
