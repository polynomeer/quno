package com.quno.qunobackend.infrastructure.security

import com.quno.qunobackend.application.token.usecase.AuthenticatePersonalAccessTokenUseCase
import com.quno.qunobackend.domain.token.PersonalAccessToken
import com.quno.qunobackend.domain.token.TokenScope
import com.quno.qunobackend.infrastructure.config.RateLimitProperties
import io.github.bucket4j.Bandwidth
import io.github.bucket4j.Bucket
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.http.HttpMethod
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.util.AntPathMatcher
import org.springframework.web.filter.OncePerRequestFilter
import java.time.Duration
import java.util.concurrent.ConcurrentHashMap

/**
 * What each token scope opens (ADR-0079). An allowlist: an endpoint nobody listed is closed to
 * tokens, so a new write endpoint can't become reachable by accident.
 */
object TokenScopePolicy {
    private val matcher = AntPathMatcher()

    /** Token management stays with the browser session, so a leaked token can't mint its own successor. */
    private val NEVER = listOf("/api/v1/me/tokens", "/api/v1/me/tokens/**")

    /** POSTs that only compute a preview and store nothing. */
    private val READ_POSTS = listOf("/api/v1/error-signatures/preview", "/api/v1/stack-fingerprints/parse")

    private val ASK_POSTS = listOf("/api/v1/questions", "/api/v1/questions/{id}/versions")

    fun allows(scopes: Set<TokenScope>, method: String, path: String): Boolean {
        if (NEVER.any { matcher.match(it, path) }) return false
        return when (method) {
            HttpMethod.GET.name() -> TokenScope.READ in scopes
            HttpMethod.POST.name() ->
                (TokenScope.READ in scopes && READ_POSTS.any { matcher.match(it, path) }) ||
                    (TokenScope.ASK in scopes && ASK_POSTS.any { matcher.match(it, path) })
            else -> false
        }
    }
}

/**
 * Authenticates `Authorization: Bearer quno_pat_…` (Phase 65.1, ADR-0079): an unknown, revoked or
 * expired token is a 401 right away — unlike a bad JWT it is never quietly downgraded to an
 * anonymous request, so a script learns its token is dead. Then the scope check (403) and a
 * per-token bucket (429, Phase 65.4). Built by SecurityConfig, not a bean, for the same
 * double-registration reason as [RateLimitFilter].
 */
class PersonalAccessTokenFilter(
    private val authenticate: AuthenticatePersonalAccessTokenUseCase,
    private val properties: RateLimitProperties,
) : OncePerRequestFilter() {

    private val buckets = ConcurrentHashMap<Long, Bucket>()

    override fun doFilterInternal(request: HttpServletRequest, response: HttpServletResponse, filterChain: FilterChain) {
        val secret = request.getHeader("Authorization")?.removePrefix("Bearer ")?.trim()
        if (secret == null || !secret.startsWith(PersonalAccessToken.SECRET_PREFIX)) {
            filterChain.doFilter(request, response)
            return
        }

        val principal = authenticate.execute(secret)
            ?: return reject(response, 401, "UNAUTHORIZED", "토큰이 유효하지 않거나 만료·폐기되었습니다.")
        if (!TokenScopePolicy.allows(principal.scopes, request.method, request.requestURI)) {
            return reject(response, 403, "FORBIDDEN", "이 토큰의 권한(${principal.scopes.sorted().joinToString()})으로는 할 수 없는 요청입니다.")
        }
        if (!buckets.computeIfAbsent(principal.tokenId) { newBucket() }.tryConsume(1)) {
            return reject(response, 429, "TOO_MANY_REQUESTS", "요청이 너무 많습니다. 잠시 후 다시 시도해 주세요.")
        }

        SecurityContextHolder.getContext().authentication = UsernamePasswordAuthenticationToken(
            principal.userId,
            null,
            listOf(SimpleGrantedAuthority(TOKEN_AUTHORITY)) + principal.scopes.map { SimpleGrantedAuthority("SCOPE_${it.name}") },
        )
        filterChain.doFilter(request, response)
    }

    private fun reject(response: HttpServletResponse, status: Int, code: String, message: String) {
        response.status = status
        response.contentType = "application/json;charset=UTF-8"
        response.writer.write("""{"code":"$code","message":"$message"}""")
    }

    private fun newBucket(): Bucket = Bucket.builder()
        .addLimit(
            Bandwidth.builder()
                .capacity(properties.tokenCapacity)
                .refillGreedy(properties.tokenCapacity, Duration.ofSeconds(properties.tokenRefillPeriodSeconds))
                .build(),
        )
        .build()

    companion object {
        const val TOKEN_AUTHORITY = "PERSONAL_ACCESS_TOKEN"
    }
}
