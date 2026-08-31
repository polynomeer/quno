package com.quno.qunobackend.interfaces.api.report

import com.quno.qunobackend.application.report.dto.ReportResult
import com.quno.qunobackend.domain.report.ReportReason
import com.quno.qunobackend.domain.report.ReportStatus
import com.quno.qunobackend.domain.report.ReportTargetType
import java.time.Instant

data class ReportResponse(
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

fun ReportResult.toResponse() = ReportResponse(
    id = id,
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
