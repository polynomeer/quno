package com.quno.qunobackend.interfaces.api.organization

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

data class CreateOrganizationRequest(
    @field:NotBlank @field:Size(max = 100)
    val name: String,
    @field:Size(max = 2000)
    val description: String? = null,
)
