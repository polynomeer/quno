package com.quno.qunobackend.infrastructure.config

import org.springframework.boot.context.properties.ConfigurationProperties

/**
 * 로그인/회원가입/결제 확인에 공통 적용하는 IP당 요청 한도. 기본값은 로컬 개발·테스트가 막히지
 * 않도록 사실상 무제한에 가깝게 두고(E2E 테스트가 같은 Spring 컨텍스트를 공유하며 로그인/회원가입을
 * 여러 번 호출한다), 운영에서만 `application-prod.yml`이 좁게 덮어쓴다. Override via
 * QUNO_RATE_LIMIT_CAPACITY / QUNO_RATE_LIMIT_REFILL_PERIOD_SECONDS.
 */
@ConfigurationProperties(prefix = "quno.rate-limit")
data class RateLimitProperties(
    val capacity: Long = 1_000,
    val refillPeriodSeconds: Long = 60,
)
