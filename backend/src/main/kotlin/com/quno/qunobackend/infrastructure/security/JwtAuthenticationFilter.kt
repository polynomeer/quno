package com.quno.qunobackend.infrastructure.security

import com.quno.qunobackend.application.user.TokenProvider
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.web.filter.OncePerRequestFilter

/**
 * Reads "Authorization: Bearer <token>", and on success sets the authenticated
 * user id as the SecurityContext principal. Leaves the request anonymous on any
 * failure so the filter chain's authorizeHttpRequests rules apply as usual.
 */
class JwtAuthenticationFilter(
    private val tokenProvider: TokenProvider,
) : OncePerRequestFilter() {

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain,
    ) {
        val header = request.getHeader("Authorization")
        if (header != null && header.startsWith(BEARER_PREFIX)) {
            val token = header.removePrefix(BEARER_PREFIX).trim()
            runCatching { tokenProvider.validateAccessToken(token) }
                .onSuccess { userId ->
                    SecurityContextHolder.getContext().authentication =
                        UsernamePasswordAuthenticationToken(userId, null, emptyList())
                }
        }
        filterChain.doFilter(request, response)
    }

    companion object {
        private const val BEARER_PREFIX = "Bearer "
    }
}
