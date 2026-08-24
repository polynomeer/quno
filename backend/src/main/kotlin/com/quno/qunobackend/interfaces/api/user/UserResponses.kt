package com.quno.qunobackend.interfaces.api.user

import java.time.Instant

data class MyProfileResponse(
    val id: Long,
    val email: String,
    val nickname: String,
    val createdAt: Instant,
)
