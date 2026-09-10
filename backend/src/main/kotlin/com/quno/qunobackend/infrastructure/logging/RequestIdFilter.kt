package com.quno.qunobackend.infrastructure.logging

import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.slf4j.MDC
import org.springframework.core.Ordered
import org.springframework.core.annotation.Order
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter
import java.util.UUID

private const val REQUEST_ID_HEADER = "X-Request-Id"
private const val MDC_KEY = "requestId"

/**
 * 요청마다 추적 ID를 부여해 로그를 관통 추적할 수 있게 한다. 클라이언트가 이미 `X-Request-Id`를
 * 보냈으면(예: 프론트엔드가 자기 쪽 상관관계 ID를 전달) 그대로 쓰고, 없으면 새로 발급한다. MDC에
 * 넣어두면 prod 프로필의 구조화 로깅(ECS 포맷)이 모든 로그 라인에 자동으로 실어준다 — Spring
 * Security 체인 밖(actuator 등)의 요청도 놓치지 않도록 `@Component`로 Boot 전역 필터로 등록하고
 * 가장 먼저 실행되게 한다(JwtAuthenticationFilter/RateLimitFilter와 달리 이건 Security 전용이
 * 아니라 모든 요청에 적용돼야 하므로 자동 등록이 맞다).
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
class RequestIdFilter : OncePerRequestFilter() {

    override fun doFilterInternal(request: HttpServletRequest, response: HttpServletResponse, filterChain: FilterChain) {
        val requestId = request.getHeader(REQUEST_ID_HEADER)?.takeIf { it.isNotBlank() } ?: UUID.randomUUID().toString()
        MDC.put(MDC_KEY, requestId)
        response.setHeader(REQUEST_ID_HEADER, requestId)
        try {
            filterChain.doFilter(request, response)
        } finally {
            MDC.remove(MDC_KEY)
        }
    }
}
