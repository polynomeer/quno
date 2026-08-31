package com.quno.qunobackend.application.report.usecase

import com.quno.qunobackend.application.report.dto.ReportResult
import com.quno.qunobackend.domain.report.ReportRepository
import com.quno.qunobackend.domain.report.ReportStatus
import com.quno.qunobackend.domain.user.UserRepository
import org.springframework.stereotype.Service

@Service
class ListReportsUseCase(
    private val userRepository: UserRepository,
    private val reportRepository: ReportRepository,
) {
    fun execute(moderatorId: Long, status: ReportStatus): List<ReportResult> {
        userRepository.requireModerator(moderatorId)
        return reportRepository.listByStatus(status).map { it.toResult() }
    }
}
