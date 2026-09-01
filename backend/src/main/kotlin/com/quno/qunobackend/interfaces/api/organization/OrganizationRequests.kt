package com.quno.qunobackend.interfaces.api.organization

import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

data class CreateOrganizationRequest(
    @field:NotBlank @field:Size(max = 100)
    val name: String,
    @field:Size(max = 2000)
    val description: String? = null,
)

data class RequestEmailDomainVerificationRequest(
    @field:NotBlank @field:Email
    val email: String,
)

data class ConfirmEmailDomainVerificationRequest(
    @field:NotBlank @field:Size(min = 6, max = 6)
    val code: String,
)
