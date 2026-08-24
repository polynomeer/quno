package com.quno.qunobackend.infrastructure.security

import com.quno.qunobackend.domain.user.InvalidTokenException
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class JwtTokenProviderTest {

    private val properties = JwtProperties(
        secret = "test-only-secret-key-0123456789-0123456789-0123456789",
        accessTokenTtlSeconds = 60,
        refreshTokenTtlSeconds = 120,
    )
    private val provider = JwtTokenProvider(properties)

    @Test
    fun `access token round-trips to the same user id`() {
        val token = provider.generateAccessToken(42L)

        assertEquals(42L, provider.validateAccessToken(token))
    }

    @Test
    fun `refresh token round-trips to the same user id`() {
        val token = provider.generateRefreshToken(7L)

        assertEquals(7L, provider.validateRefreshToken(token))
    }

    @Test
    fun `a refresh token is rejected when validated as an access token`() {
        val refreshToken = provider.generateRefreshToken(42L)

        assertFailsWith<InvalidTokenException> { provider.validateAccessToken(refreshToken) }
    }

    @Test
    fun `an access token is rejected when validated as a refresh token`() {
        val accessToken = provider.generateAccessToken(42L)

        assertFailsWith<InvalidTokenException> { provider.validateRefreshToken(accessToken) }
    }

    @Test
    fun `a garbage token is rejected`() {
        assertFailsWith<InvalidTokenException> { provider.validateAccessToken("not-a-jwt") }
    }
}
