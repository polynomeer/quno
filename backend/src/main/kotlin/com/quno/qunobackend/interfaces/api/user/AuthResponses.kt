package com.quno.qunobackend.interfaces.api.user

data class SignUpResponse(val id: Long, val email: String, val nickname: String)

data class TokenResponse(
    val accessToken: String,
    val refreshToken: String,
    val tokenType: String = "Bearer",
)
