package com.quno.qunobackend.interfaces.api.report

import com.quno.qunobackend.domain.report.ReportReason
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Size

data class FileReportRequest(
    @field:NotNull
    val reason: ReportReason,

    @field:Size(max = 1000)
    val message: String? = null,
)
