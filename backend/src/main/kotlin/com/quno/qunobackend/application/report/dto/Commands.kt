package com.quno.qunobackend.application.report.dto

import com.quno.qunobackend.domain.report.ReportReason
import com.quno.qunobackend.domain.report.ReportTargetType

data class FileReportCommand(
    val reporterId: Long,
    val targetType: ReportTargetType,
    val targetId: Long,
    val reason: ReportReason,
    val message: String?,
)
