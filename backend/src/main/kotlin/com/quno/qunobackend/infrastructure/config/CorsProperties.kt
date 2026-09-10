package com.quno.qunobackend.infrastructure.config

import org.springframework.boot.context.properties.ConfigurationProperties

/** Override via QUNO_CORS_ALLOWED_ORIGINS (comma-separated) outside local dev. */
@ConfigurationProperties(prefix = "quno.cors")
data class CorsProperties(
    val allowedOrigins: List<String> = listOf("http://localhost:3000"),
)
