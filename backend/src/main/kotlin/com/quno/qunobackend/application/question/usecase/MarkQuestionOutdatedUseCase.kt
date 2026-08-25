package com.quno.qunobackend.application.question.usecase

import com.quno.qunobackend.application.question.dto.MarkQuestionOutdatedCommand
import com.quno.qunobackend.application.question.dto.QuestionMutationResult
import com.quno.qunobackend.domain.common.OutboxEvent
import com.quno.qunobackend.domain.common.OutboxEventRepository
import com.quno.qunobackend.domain.common.OutboxEventTypes
import com.quno.qunobackend.domain.question.QuestionNotFoundException
import com.quno.qunobackend.domain.question.QuestionRepository
import com.quno.qunobackend.domain.question.QuestionVersionRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * Flags a question as outdated on a user's say-so (PLAN.md 8.1, ADR-0017) — there is no
 * automatic technology-version-change detection behind this; anyone can call it, including
 * the question's own author.
 */
@Service
class MarkQuestionOutdatedUseCase(
    private val questionRepository: QuestionRepository,
    private val questionVersionRepository: QuestionVersionRepository,
    private val outboxEventRepository: OutboxEventRepository,
) {
    @Transactional
    fun execute(command: MarkQuestionOutdatedCommand): QuestionMutationResult {
        val question = questionRepository.findById(command.questionId)
            ?: throw QuestionNotFoundException(command.questionId)
        val updated = questionRepository.save(question.markOutdated())
        val versionNumber = requireNotNull(
            questionVersionRepository.findById(requireNotNull(question.latestVersionId)),
        ).versionNumber

        // questionAuthorId: the question's author is always notified, even if they never
        // explicitly watched their own question — see DispatchOutboxEventsUseCase's kdoc.
        outboxEventRepository.save(
            OutboxEvent.create(
                eventType = OutboxEventTypes.QUESTION_OUTDATED,
                aggregateType = "QUESTION",
                aggregateId = command.questionId,
                payload = """{"actorId":${command.actorId},"questionAuthorId":${question.authorId},"reason":"${escapeJson(command.reason)}"}""",
            ),
        )

        return QuestionMutationResult(id = command.questionId, title = updated.title, status = updated.status, versionNumber = versionNumber)
    }

    private fun escapeJson(value: String): String =
        value.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "")
}
