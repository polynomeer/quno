package com.quno.qunobackend.infrastructure.security

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "quno.jwt")
data class JwtProperties(
    /** HMAC signing secret. Override via QUNO_JWT_SECRET outside local dev. */
    val secret: String,
    val accessTokenTtlSeconds: Long = 1_800,
    val refreshTokenTtlSeconds: Long = 1_209_600,
)
