package com.quno.qunobackend.application.report.usecase

import com.quno.qunobackend.domain.report.Report
import com.quno.qunobackend.domain.report.ReportRepository
import com.quno.qunobackend.domain.report.ReportStatus
import com.quno.qunobackend.domain.report.ReportTargetType

class InMemoryReportRepository : ReportRepository {
    private val byId = mutableMapOf<Long, Report>()
    private var nextId = 1L

    override fun save(report: Report): Report {
        val saved = if (report.id == null) {
            Report.reconstitute(
                id = nextId++,
                reporterId = report.reporterId,
                targetType = report.targetType,
                targetId = report.targetId,
                reason = report.reason,
                message = report.message,
                status = report.status,
                resolvedBy = report.resolvedBy,
                resolvedAt = report.resolvedAt,
                createdAt = report.createdAt,
            )
        } else {
            report
        }
        byId[requireNotNull(saved.id)] = saved
        return saved
    }

    override fun findById(id: Long): Report? = byId[id]

    override fun listByStatus(status: ReportStatus): List<Report> =
        byId.values.filter { it.status == status }.sortedBy { it.createdAt }

    override fun countByTarget(targetType: ReportTargetType, targetId: Long): Long =
        byId.values.count { it.targetType == targetType && it.targetId == targetId }.toLong()
}
