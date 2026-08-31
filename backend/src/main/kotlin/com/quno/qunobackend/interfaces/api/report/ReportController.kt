package com.quno.qunobackend.interfaces.api.report

import com.quno.qunobackend.application.report.dto.FileReportCommand
import com.quno.qunobackend.application.report.usecase.FileReportUseCase
import com.quno.qunobackend.domain.report.ReportTargetType
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

/** Anyone can report — even the content's own author (no self-report restriction, unlike Vote). */
@RestController
@RequestMapping("/api/v1")
class ReportController(
    private val fileReportUseCase: FileReportUseCase,
) {

    @PostMapping("/questions/{questionId}/reports")
    @ResponseStatus(HttpStatus.CREATED)
    fun reportQuestion(
        @AuthenticationPrincipal reporterId: Long,
        @PathVariable questionId: Long,
        @Valid @RequestBody request: FileReportRequest,
    ): ReportResponse = fileReportUseCase.execute(
        FileReportCommand(reporterId, ReportTargetType.QUESTION, questionId, request.reason, request.message),
    ).toResponse()

    @PostMapping("/answers/{answerId}/reports")
    @ResponseStatus(HttpStatus.CREATED)
    fun reportAnswer(
        @AuthenticationPrincipal reporterId: Long,
        @PathVariable answerId: Long,
        @Valid @RequestBody request: FileReportRequest,
    ): ReportResponse = fileReportUseCase.execute(
        FileReportCommand(reporterId, ReportTargetType.ANSWER, answerId, request.reason, request.message),
    ).toResponse()
}
