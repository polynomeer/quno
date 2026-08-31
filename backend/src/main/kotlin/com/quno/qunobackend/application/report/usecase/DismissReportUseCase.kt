package com.quno.qunobackend.application.report.usecase

import com.quno.qunobackend.domain.report.ReportNotFoundException
import com.quno.qunobackend.domain.report.ReportRepository
import com.quno.qunobackend.domain.user.UserRepository
import org.springframework.stereotype.Service

/** Covers design.md's "Keep" action too — the content is left untouched either way, only the
 * report is closed (ADR-0028 treats Keep and Dismiss as the same outcome). */
@Service
class DismissReportUseCase(
    private val userRepository: UserRepository,
    private val reportRepository: ReportRepository,
) {
    fun execute(moderatorId: Long, reportId: Long) {
        userRepository.requireModerator(moderatorId)
        val report = reportRepository.findById(reportId) ?: throw ReportNotFoundException(reportId)
        reportRepository.save(report.dismiss(moderatorId))
    }
}
