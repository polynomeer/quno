package com.quno.qunobackend.application.answer.usecase

import com.quno.qunobackend.application.answer.dto.AnswerMutationResult
import com.quno.qunobackend.application.answer.dto.ReviseAnswerCommand
import com.quno.qunobackend.domain.answer.AnswerAccessDeniedException
import com.quno.qunobackend.domain.answer.AnswerNotFoundException
import com.quno.qunobackend.domain.answer.AnswerRepository
import com.quno.qunobackend.domain.answer.AnswerVersion
import com.quno.qunobackend.domain.answer.AnswerVersionRepository
import com.quno.qunobackend.domain.common.OutboxEvent
import com.quno.qunobackend.domain.common.OutboxEventRepository
import com.quno.qunobackend.domain.common.OutboxEventTypes
import com.quno.qunobackend.domain.question.QuestionRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * Appends a new AnswerVersion (Av2+). No pessimistic locking, unlike ReviseQuestionUseCase — only
 * the answer's own author can ever revise it, so there's no comparable concurrent-edit pressure
 * to guard against (Phase 17, ADR-0029).
 */
@Service
class ReviseAnswerUseCase(
    private val answerRepository: AnswerRepository,
    private val answerVersionRepository: AnswerVersionRepository,
    private val questionRepository: QuestionRepository,
    private val outboxEventRepository: OutboxEventRepository,
) {
    @Transactional
    fun execute(command: ReviseAnswerCommand): AnswerMutationResult {
        val answer = answerRepository.findById(command.answerId) ?: throw AnswerNotFoundException(command.answerId)
        if (answer.authorId != command.actorId) throw AnswerAccessDeniedException(command.answerId)

        val latestVersionId = requireNotNull(answer.latestVersionId) {
            "answer ${command.answerId} has no latest version yet"
        }
        val latestVersion = answerVersionRepository.findById(latestVersionId)
            ?: throw AnswerNotFoundException(command.answerId)

        val newVersion = answerVersionRepository.save(
            AnswerVersion.create(
                answerId = command.answerId,
                versionNumber = latestVersion.versionNumber + 1,
                bodyMarkdown = command.body,
                createdBy = command.actorId,
            ),
        )

        answerRepository.save(answer.withLatestVersion(requireNotNull(newVersion.id), bodyMarkdown = command.body))

        // questionAuthorId is omitted (not thrown) when the question is gone — e.g. hidden by
        // moderation (Phase 16) — revising your own answer shouldn't depend on that.
        val questionAuthorId = questionRepository.findById(answer.questionId)?.authorId
        outboxEventRepository.save(
            OutboxEvent.create(
                eventType = OutboxEventTypes.ANSWER_REVISION,
                aggregateType = "QUESTION",
                aggregateId = answer.questionId,
                payload = buildString {
                    append("""{"answerId":${command.answerId},"versionNumber":${newVersion.versionNumber},"actorId":${command.actorId}""")
                    if (questionAuthorId != null) append(""","questionAuthorId":$questionAuthorId""")
                    append("}")
                },
            ),
        )

        return AnswerMutationResult(id = command.answerId, questionId = answer.questionId, versionNumber = newVersion.versionNumber)
    }
}
