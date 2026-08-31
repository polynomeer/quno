package com.quno.qunobackend.interfaces.api.moderation

import com.quno.qunobackend.application.report.usecase.DismissReportUseCase
import com.quno.qunobackend.application.report.usecase.HideReportedContentUseCase
import com.quno.qunobackend.application.report.usecase.ListReportsUseCase
import com.quno.qunobackend.domain.report.ReportStatus
import com.quno.qunobackend.interfaces.api.report.ReportResponse
import com.quno.qunobackend.interfaces.api.report.toResponse
import org.springframework.http.HttpStatus
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

/** Moderator-only — role is checked in each use case (ADR-0028), not via a security filter. */
@RestController
@RequestMapping("/api/v1/moderation/reports")
class ModerationController(
    private val listReportsUseCase: ListReportsUseCase,
    private val dismissReportUseCase: DismissReportUseCase,
    private val hideReportedContentUseCase: HideReportedContentUseCase,
) {

    @GetMapping
    fun list(
        @AuthenticationPrincipal moderatorId: Long,
        @RequestParam(defaultValue = "PENDING") status: ReportStatus,
    ): List<ReportResponse> = listReportsUseCase.execute(moderatorId, status).map { it.toResponse() }

    @PostMapping("/{reportId}/dismiss")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun dismiss(@AuthenticationPrincipal moderatorId: Long, @PathVariable reportId: Long) {
        dismissReportUseCase.execute(moderatorId, reportId)
    }

    @PostMapping("/{reportId}/hide")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun hide(@AuthenticationPrincipal moderatorId: Long, @PathVariable reportId: Long) {
        hideReportedContentUseCase.execute(moderatorId, reportId)
    }
}
