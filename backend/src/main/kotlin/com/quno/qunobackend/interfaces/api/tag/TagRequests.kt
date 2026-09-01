package com.quno.qunobackend.interfaces.api.tag

import jakarta.validation.constraints.Size

data class UpdateTagDetailsRequest(
    @field:Size(max = 2000)
    val description: String? = null,
    @field:Size(max = 500)
    val docsUrl: String? = null,
)
