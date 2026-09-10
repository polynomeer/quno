package com.quno.qunobackend.infrastructure.config

import io.swagger.v3.oas.models.Components
import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.info.Info
import io.swagger.v3.oas.models.security.SecurityRequirement
import io.swagger.v3.oas.models.security.SecurityScheme
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

/** springdoc-openapi가 컨트롤러에서 자동 생성하는 문서에 최소한의 메타데이터와 JWT Bearer
 * 인증 스킴을 붙인다(ADR-0048) — 스킴을 등록해야 Swagger UI의 "Authorize" 버튼으로 토큰을 넣고
 * 인증이 필요한 엔드포인트를 직접 호출해볼 수 있다. */
@Configuration
class OpenApiConfig {

    @Bean
    fun openApi(): OpenAPI {
        val schemeName = "bearerAuth"
        return OpenAPI()
            .info(
                Info()
                    .title("Quno API")
                    .description("개발자를 위한 살아있는 Q&A 플랫폼 API 문서")
                    .version("v1"),
            )
            .addSecurityItem(SecurityRequirement().addList(schemeName))
            .components(
                Components()
                    .addSecuritySchemes(
                        schemeName,
                        SecurityScheme()
                            .name(schemeName)
                            .type(SecurityScheme.Type.HTTP)
                            .scheme("bearer")
                            .bearerFormat("JWT"),
                    ),
            )
    }
}
