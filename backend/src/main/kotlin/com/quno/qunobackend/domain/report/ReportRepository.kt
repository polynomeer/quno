package com.quno.qunobackend.domain.report

interface ReportRepository {
    fun save(report: Report): Report
    fun findById(id: Long): Report?
    fun listByStatus(status: ReportStatus): List<Report>
    fun countByTarget(targetType: ReportTargetType, targetId: Long): Long
}
