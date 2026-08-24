package com.quno.qunobackend.infrastructure.security

import com.quno.qunobackend.application.user.TokenProvider
import com.quno.qunobackend.domain.user.InvalidTokenException
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.security.Keys
import org.springframework.stereotype.Component
import java.time.Instant
import java.util.Date
import javax.crypto.SecretKey

@Component
class JwtTokenProvider(
    jwtProperties: JwtProperties,
) : TokenProvider {

    private val key: SecretKey = Keys.hmacShaKeyFor(jwtProperties.secret.toByteArray())
    private val accessTokenTtlSeconds = jwtProperties.accessTokenTtlSeconds
    private val refreshTokenTtlSeconds = jwtProperties.refreshTokenTtlSeconds

    override fun generateAccessToken(userId: Long): String =
        generateToken(userId, TOKEN_TYPE_ACCESS, accessTokenTtlSeconds)

    override fun generateRefreshToken(userId: Long): String =
        generateToken(userId, TOKEN_TYPE_REFRESH, refreshTokenTtlSeconds)

    override fun validateAccessToken(token: String): Long = parseAndValidate(token, TOKEN_TYPE_ACCESS)

    override fun validateRefreshToken(token: String): Long = parseAndValidate(token, TOKEN_TYPE_REFRESH)

    private fun generateToken(userId: Long, type: String, ttlSeconds: Long): String {
        val now = Instant.now()
        return Jwts.builder()
            .subject(userId.toString())
            .claim(CLAIM_TYPE, type)
            .issuedAt(Date.from(now))
            .expiration(Date.from(now.plusSeconds(ttlSeconds)))
            .signWith(key)
            .compact()
    }

    private fun parseAndValidate(token: String, expectedType: String): Long {
        try {
            val claims = Jwts.parser().verifyWith(key).build().parseSignedClaims(token).payload
            val type = claims.get(CLAIM_TYPE, String::class.java)
            if (type != expectedType) throw InvalidTokenException()
            return claims.subject.toLong()
        } catch (ex: InvalidTokenException) {
            throw ex
        } catch (ex: Exception) {
            throw InvalidTokenException()
        }
    }

    companion object {
        private const val CLAIM_TYPE = "type"
        private const val TOKEN_TYPE_ACCESS = "access"
        private const val TOKEN_TYPE_REFRESH = "refresh"
    }
}
