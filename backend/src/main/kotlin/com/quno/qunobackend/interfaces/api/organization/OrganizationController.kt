package com.quno.qunobackend.interfaces.api.organization

import com.quno.qunobackend.application.organization.usecase.ConfirmEmailDomainVerificationUseCase
import com.quno.qunobackend.application.organization.usecase.CreateOrganizationUseCase
import com.quno.qunobackend.application.organization.usecase.GetOrganizationUseCase
import com.quno.qunobackend.application.organization.usecase.JoinOrganizationUseCase
import com.quno.qunobackend.application.organization.usecase.LeaveOrganizationUseCase
import com.quno.qunobackend.application.organization.usecase.RequestEmailDomainVerificationUseCase
import com.quno.qunobackend.application.organization.usecase.SearchOrganizationsUseCase
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

/** Virtual/Community organizations (Phase 22, ADR-0034) — no external identity verification,
 * same trust level as creating a Tag. Verified organizations (Phase 23, ADR-0035) can only be
 * joined through `/verify-email` + `/verify-email/confirm`, never through `POST /{id}/join`. */
@RestController
@RequestMapping("/api/v1/organizations")
class OrganizationController(
    private val createOrganizationUseCase: CreateOrganizationUseCase,
    private val searchOrganizationsUseCase: SearchOrganizationsUseCase,
    private val getOrganizationUseCase: GetOrganizationUseCase,
    private val joinOrganizationUseCase: JoinOrganizationUseCase,
    private val leaveOrganizationUseCase: LeaveOrganizationUseCase,
    private val requestEmailDomainVerificationUseCase: RequestEmailDomainVerificationUseCase,
    private val confirmEmailDomainVerificationUseCase: ConfirmEmailDomainVerificationUseCase,
) {

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun create(@AuthenticationPrincipal userId: Long, @Valid @RequestBody request: CreateOrganizationRequest): OrganizationResponse =
        createOrganizationUseCase.execute(request.name, request.description, userId).toResponse()

    @GetMapping
    fun search(@RequestParam(required = false) q: String?, @RequestParam(required = false) limit: Int?): List<OrganizationResponse> =
        searchOrganizationsUseCase.execute(q, limit ?: 20).map { it.toResponse() }

    @GetMapping("/{id}")
    fun get(@PathVariable id: Long): OrganizationResponse = getOrganizationUseCase.execute(id).toResponse()

    @PostMapping("/{id}/join")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun join(@AuthenticationPrincipal userId: Long, @PathVariable id: Long) {
        joinOrganizationUseCase.execute(userId, id)
    }

    @DeleteMapping("/{id}/join")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun leave(@AuthenticationPrincipal userId: Long, @PathVariable id: Long) {
        leaveOrganizationUseCase.execute(userId, id)
    }

    @PostMapping("/verify-email")
    fun requestEmailDomainVerification(
        @AuthenticationPrincipal userId: Long,
        @Valid @RequestBody request: RequestEmailDomainVerificationRequest,
    ): EmailDomainVerificationResponse = requestEmailDomainVerificationUseCase.execute(userId, request.email).toResponse()

    @PostMapping("/verify-email/confirm")
    fun confirmEmailDomainVerification(
        @AuthenticationPrincipal userId: Long,
        @Valid @RequestBody request: ConfirmEmailDomainVerificationRequest,
    ): OrganizationResponse = confirmEmailDomainVerificationUseCase.execute(userId, request.code).toResponse()
}
