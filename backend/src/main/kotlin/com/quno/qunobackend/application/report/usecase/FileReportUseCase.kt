package com.quno.qunobackend.application.report.usecase

import com.quno.qunobackend.application.report.dto.FileReportCommand
import com.quno.qunobackend.application.report.dto.ReportResult
import com.quno.qunobackend.domain.answer.AnswerNotFoundException
import com.quno.qunobackend.domain.answer.AnswerRepository
import com.quno.qunobackend.domain.question.QuestionNotFoundException
import com.quno.qunobackend.domain.question.QuestionRepository
import com.quno.qunobackend.domain.report.Report
import com.quno.qunobackend.domain.report.ReportRepository
import com.quno.qunobackend.domain.report.ReportTargetType
import org.springframework.stereotype.Service

@Service
class FileReportUseCase(
    private val questionRepository: QuestionRepository,
    private val answerRepository: AnswerRepository,
    private val reportRepository: ReportRepository,
) {
    fun execute(command: FileReportCommand): ReportResult {
        when (command.targetType) {
            ReportTargetType.QUESTION ->
                questionRepository.findById(command.targetId) ?: throw QuestionNotFoundException(command.targetId)
            ReportTargetType.ANSWER ->
                answerRepository.findById(command.targetId) ?: throw AnswerNotFoundException(command.targetId)
        }

        val saved = reportRepository.save(
            Report.file(command.reporterId, command.targetType, command.targetId, command.reason, command.message),
        )
        return saved.toResult()
    }
}

internal fun Report.toResult() = ReportResult(
    id = requireNotNull(id),
    reporterId = reporterId,
    targetType = targetType,
    targetId = targetId,
    reason = reason,
    message = message,
    status = status,
    resolvedBy = resolvedBy,
    resolvedAt = resolvedAt,
    createdAt = createdAt,
)
