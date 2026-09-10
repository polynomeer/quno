package com.quno.qunobackend.infrastructure.security

import com.quno.qunobackend.infrastructure.config.RateLimitProperties
import io.github.bucket4j.Bandwidth
import io.github.bucket4j.Bucket
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.http.HttpMethod
import org.springframework.util.AntPathMatcher
import org.springframework.web.filter.OncePerRequestFilter
import java.time.Duration
import java.util.concurrent.ConcurrentHashMap

private val PROTECTED_PATTERNS = listOf(
    HttpMethod.POST to "/api/v1/auth/signup",
    HttpMethod.POST to "/api/v1/auth/login",
    HttpMethod.POST to "/api/v1/auth/refresh",
    HttpMethod.POST to "/api/v1/direct-asks/payments/confirm",
)

/**
 * 로그인/회원가입/결제 확인처럼 무차별 대입·남용에 취약한 엔드포인트를 클라이언트 IP당 인메모리
 * 토큰 버킷으로 제한한다. `JwtAuthenticationFilter`와 같은 방식으로(빈으로 등록하지 않고)
 * `SecurityConfig`가 직접 생성해 체인에 끼워 넣는다 — `@Component`로 등록하면 Spring Boot가
 * 별도로 한 번 더 서블릿 필터로 자동 등록해 같은 필터가 두 파이프라인에 중복으로 물린다.
 *
 * 한도는 [RateLimitProperties]로 주입받는다 — E2E 테스트가 같은 Spring 컨텍스트를 공유하며
 * 로그인/회원가입을 여러 번 호출하므로, 로컬/테스트 기본값은 사실상 무제한에 가깝고 운영에서만
 * 좁게 덮어쓴다(`application-prod.yml`).
 *
 * 인메모리 버킷이라 단일 인스턴스 기준이다. 다중 인스턴스로 스케일아웃하면 인스턴스마다 한도가
 * 독립적으로 리셋되어 실효 한도가 인스턴스 수만큼 늘어나므로, 그때는 Redis 백엔드로 바꿔야 한다
 * (production-readiness.md B-2).
 */
class RateLimitFilter(
    private val properties: RateLimitProperties,
) : OncePerRequestFilter() {

    private val pathMatcher = AntPathMatcher()
    private val buckets = ConcurrentHashMap<String, Bucket>()

    override fun doFilterInternal(request: HttpServletRequest, response: HttpServletResponse, filterChain: FilterChain) {
        val method = HttpMethod.valueOf(request.method)
        val matched = PROTECTED_PATTERNS.firstOrNull { (m, pattern) -> m == method && pathMatcher.match(pattern, request.requestURI) }
        if (matched == null) {
            filterChain.doFilter(request, response)
            return
        }

        val key = "${matched.first}:${matched.second}:${clientIp(request)}"
        val bucket = buckets.computeIfAbsent(key) { newBucket() }
        if (bucket.tryConsume(1)) {
            filterChain.doFilter(request, response)
        } else {
            response.status = 429
            response.contentType = "application/json;charset=UTF-8"
            response.writer.write(
                """{"code":"TOO_MANY_REQUESTS","message":"요청이 너무 많습니다. 잠시 후 다시 시도해 주세요."}""",
            )
        }
    }

    private fun newBucket(): Bucket {
        val bandwidth = Bandwidth.builder()
            .capacity(properties.capacity)
            .refillGreedy(properties.capacity, Duration.ofSeconds(properties.refillPeriodSeconds))
            .build()
        return Bucket.builder().addLimit(bandwidth).build()
    }

    private fun clientIp(request: HttpServletRequest): String =
        request.getHeader("X-Forwarded-For")
            ?.substringBefore(",")
            ?.trim()
            ?.takeIf { it.isNotBlank() }
            ?: request.remoteAddr
}
