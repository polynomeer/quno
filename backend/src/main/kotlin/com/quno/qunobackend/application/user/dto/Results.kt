package com.quno.qunobackend.application.user.dto

import java.time.Instant

data class SignUpResult(val userId: Long, val email: String, val nickname: String)

data class TokenResult(val accessToken: String, val refreshToken: String)

data class MyProfileResult(val id: Long, val email: String, val nickname: String, val createdAt: Instant)
