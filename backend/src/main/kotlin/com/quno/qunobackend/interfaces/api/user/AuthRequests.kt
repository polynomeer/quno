package com.quno.qunobackend.interfaces.api.user

import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

data class SignUpRequest(
    @field:NotBlank @field:Email
    val email: String,
    @field:NotBlank @field:Size(min = 2, max = 50)
    val nickname: String,
    @field:NotBlank @field:Size(min = 8, max = 100)
    val password: String,
)

data class LoginRequest(
    @field:NotBlank @field:Email
    val email: String,
    @field:NotBlank
    val password: String,
)

data class RefreshRequest(
    @field:NotBlank
    val refreshToken: String,
)
