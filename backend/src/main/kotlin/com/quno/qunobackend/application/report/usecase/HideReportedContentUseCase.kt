package com.quno.qunobackend.application.report.usecase

import com.quno.qunobackend.domain.answer.AnswerNotFoundException
import com.quno.qunobackend.domain.answer.AnswerRepository
import com.quno.qunobackend.domain.common.OutboxEvent
import com.quno.qunobackend.domain.common.OutboxEventRepository
import com.quno.qunobackend.domain.common.OutboxEventTypes
import com.quno.qunobackend.domain.question.QuestionNotFoundException
import com.quno.qunobackend.domain.question.QuestionRepository
import com.quno.qunobackend.domain.report.ReportNotFoundException
import com.quno.qunobackend.domain.report.ReportRepository
import com.quno.qunobackend.domain.report.ReportTargetType
import com.quno.qunobackend.domain.user.UserRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * Soft-deletes the reported Question/Answer and closes the report as ACTIONED. Does not cascade
 * — hiding a question does not hide its answers (ADR-0028, kept simple on purpose). Notifies only
 * the hidden content's author, not the question's Ward subscribers (see
 * DispatchOutboxEventsUseCase kdoc).
 */
@Service
class HideReportedContentUseCase(
    private val userRepository: UserRepository,
    private val questionRepository: QuestionRepository,
    private val answerRepository: AnswerRepository,
    private val reportRepository: ReportRepository,
    private val outboxEventRepository: OutboxEventRepository,
) {
    @Transactional
    fun execute(moderatorId: Long, reportId: Long) {
        userRepository.requireModerator(moderatorId)
        val report = reportRepository.findById(reportId) ?: throw ReportNotFoundException(reportId)

        // Validate/transition the report's own state first — a report that's already been
        // acted on (e.g. the same content hidden by an earlier report) means the target may
        // already be gone, which would otherwise surface as a confusing 404 instead of 409.
        reportRepository.save(report.action(moderatorId))

        val hidden = when (report.targetType) {
            ReportTargetType.QUESTION -> {
                val question = questionRepository.findById(report.targetId) ?: throw QuestionNotFoundException(report.targetId)
                questionRepository.save(question.softDelete())
                HiddenContent(aggregateQuestionId = requireNotNull(question.id), authorId = question.authorId, answerId = null)
            }
            ReportTargetType.ANSWER -> {
                val answer = answerRepository.findById(report.targetId) ?: throw AnswerNotFoundException(report.targetId)
                answerRepository.save(answer.softDelete())
                HiddenContent(aggregateQuestionId = answer.questionId, authorId = answer.authorId, answerId = answer.id)
            }
        }

        outboxEventRepository.save(
            OutboxEvent.create(
                eventType = OutboxEventTypes.CONTENT_HIDDEN,
                aggregateType = "QUESTION",
                aggregateId = hidden.aggregateQuestionId,
                payload = buildString {
                    append("""{"actorId":$moderatorId,"contentAuthorId":${hidden.authorId}""")
                    if (hidden.answerId != null) append(""","answerId":${hidden.answerId}""")
                    append("}")
                },
            ),
        )
    }

    private data class HiddenContent(val aggregateQuestionId: Long, val authorId: Long, val answerId: Long?)
}
