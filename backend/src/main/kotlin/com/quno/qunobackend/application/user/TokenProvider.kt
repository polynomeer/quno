package com.quno.qunobackend.application.user

/** Port implemented by infrastructure/security/JwtTokenProvider. */
interface TokenProvider {
    fun generateAccessToken(userId: Long): String
    fun generateRefreshToken(userId: Long): String

    /** @throws com.quno.qunobackend.domain.user.InvalidTokenException if invalid, expired, or wrong token type. */
    fun validateAccessToken(token: String): Long

    /** @throws com.quno.qunobackend.domain.user.InvalidTokenException if invalid, expired, or wrong token type. */
    fun validateRefreshToken(token: String): Long
}
