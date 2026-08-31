package com.quno.qunobackend.application.report.dto

import com.quno.qunobackend.domain.report.ReportReason
import com.quno.qunobackend.domain.report.ReportStatus
import com.quno.qunobackend.domain.report.ReportTargetType
import java.time.Instant

data class ReportResult(
    val id: Long,
    val reporterId: Long,
    val targetType: ReportTargetType,
    val targetId: Long,
    val reason: ReportReason,
    val message: String?,
    val status: ReportStatus,
    val resolvedBy: Long?,
    val resolvedAt: Instant?,
    val createdAt: Instant,
)
