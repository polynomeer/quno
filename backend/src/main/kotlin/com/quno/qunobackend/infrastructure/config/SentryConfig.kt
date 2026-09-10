package com.quno.qunobackend.infrastructure.config

import io.sentry.Sentry
import jakarta.annotation.PostConstruct
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component

/**
 * `sentry-spring-boot-starter-jakarta`는 아직 Spring Boot 4의 재구성된 패키지와 호환되지
 * 않아(`RestClientCustomizer` `ClassNotFoundException`으로 ApplicationContext 로드 자체가
 * 실패하는 것을 테스트로 확인) 코어 SDK(`io.sentry:sentry`)만 의존성에 두고 여기서 직접
 * 초기화한다. DSN이 비어 있으면(기본값) 아무것도 하지 않는다 — 계정 개설·DSN 발급은 사람이
 * 할 일(production-readiness.md A)이라 이게 없어도 앱이 정상 기동해야 한다.
 */
@Component
class SentryConfig(
    @Value("\${sentry.dsn:}") private val dsn: String,
    @Value("\${sentry.environment:local}") private val environment: String,
) {

    @PostConstruct
    fun init() {
        if (dsn.isBlank()) return
        Sentry.init { options ->
            options.dsn = dsn
            options.environment = environment
        }
    }
}
