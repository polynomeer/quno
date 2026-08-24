package com.quno.qunobackend.application.user.dto

data class SignUpCommand(val email: String, val nickname: String, val rawPassword: String)

data class LoginCommand(val email: String, val rawPassword: String)

data class RefreshTokenCommand(val refreshToken: String)
