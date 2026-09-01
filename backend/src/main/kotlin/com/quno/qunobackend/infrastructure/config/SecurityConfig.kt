package com.quno.qunobackend.infrastructure.config

import com.quno.qunobackend.application.user.TokenProvider
import com.quno.qunobackend.infrastructure.security.JwtAuthenticationFilter
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.invoke
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.authentication.HttpStatusEntryPoint
import org.springframework.web.cors.CorsConfiguration
import org.springframework.web.cors.CorsConfigurationSource
import org.springframework.web.cors.UrlBasedCorsConfigurationSource

/** Baseline chain for a stateless, JWT-authenticated API (see docs/architecture/api-design.md). */
@Configuration
class SecurityConfig(
    private val tokenProvider: TokenProvider,
) {

    @Bean
    fun passwordEncoder(): PasswordEncoder = BCryptPasswordEncoder()

    /** Allows the frontend/ dev server (see docs/frontend/) to call this API from the browser. */
    @Bean
    fun corsConfigurationSource(): CorsConfigurationSource {
        val configuration = CorsConfiguration().apply {
            allowedOrigins = listOf("http://localhost:3000")
            allowedMethods = listOf("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
            allowedHeaders = listOf("*")
            allowCredentials = true
        }
        return UrlBasedCorsConfigurationSource().apply {
            registerCorsConfiguration("/**", configuration)
        }
    }

    @Bean
    fun filterChain(http: HttpSecurity): SecurityFilterChain {
        http.addFilterBefore(JwtAuthenticationFilter(tokenProvider), UsernamePasswordAuthenticationFilter::class.java)
        http {
            cors { configurationSource = corsConfigurationSource() }
            csrf { disable() }
            sessionManagement { sessionCreationPolicy = SessionCreationPolicy.STATELESS }
            httpBasic { disable() }
            formLogin { disable() }
            exceptionHandling {
                authenticationEntryPoint = HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED)
            }
            authorizeHttpRequests {
                // Boot forwards uncaught exceptions here; without this, that internal forward hits
                // this same chain unauthenticated and the client sees a misleading 401 instead of
                // the real error status (found via a uq_tags_slug_active violation surfacing as 401).
                authorize("/error", permitAll)
                authorize("/actuator/health", permitAll)
                authorize("/actuator/info", permitAll)
                authorize("/api/v1/auth/**", permitAll)
                // The WebSocket handshake itself stays unauthenticated — real auth happens one
                // level up, inside the STOMP CONNECT frame (see StompAuthChannelInterceptor).
                authorize("/ws/**", permitAll)
                // Public read access, part 1 (Phase 29, ADR-0041) — question/answer/comment
                // content and search, so an anonymous visitor can actually read what a shared
                // link points to. Deliberately narrow: only these GETs, method-scoped so the
                // POST/PUT/DELETE on the very same paths (revise, comment, etc.) still require
                // auth. Part 2 (tags/organizations/profiles) is the next block down. Direct-asks,
                // live-chat, dashboard, moderation, and notifications still stay behind the
                // anyRequest/authenticated fallback below — not opened by either part.
                authorize(HttpMethod.GET, "/api/v1/questions/{id}", permitAll)
                authorize(HttpMethod.GET, "/api/v1/questions/{id}/versions", permitAll)
                authorize(HttpMethod.GET, "/api/v1/questions/{id}/versions/{version}", permitAll)
                authorize(HttpMethod.GET, "/api/v1/questions/{id}/versions/{version}/diff", permitAll)
                authorize(HttpMethod.GET, "/api/v1/questions/{id}/related", permitAll)
                authorize(HttpMethod.GET, "/api/v1/questions/{questionId}/answers", permitAll)
                authorize(HttpMethod.GET, "/api/v1/questions/{questionId}/comments", permitAll)
                authorize(HttpMethod.GET, "/api/v1/answers/{answerId}/versions", permitAll)
                authorize(HttpMethod.GET, "/api/v1/answers/{answerId}/versions/{version}", permitAll)
                authorize(HttpMethod.GET, "/api/v1/answers/{answerId}/versions/{version}/diff", permitAll)
                authorize(HttpMethod.GET, "/api/v1/answers/{answerId}/comments", permitAll)
                authorize(HttpMethod.GET, "/api/v1/comments/{commentId}/versions", permitAll)
                authorize(HttpMethod.GET, "/api/v1/search", permitAll)
                // Public read access, part 2 (Phase 30, ADR-0042) — tags, organizations, user
                // profiles. Same method-scoped principle: the write verbs on these same paths
                // (tag edit, org create/join/leave, follow) still require auth.
                authorize(HttpMethod.GET, "/api/v1/tags", permitAll)
                authorize(HttpMethod.GET, "/api/v1/tags/{id}", permitAll)
                authorize(HttpMethod.GET, "/api/v1/tags/{id}/questions", permitAll)
                authorize(HttpMethod.GET, "/api/v1/tags/{id}/contributors", permitAll)
                authorize(HttpMethod.GET, "/api/v1/tags/{id}/related", permitAll)
                authorize(HttpMethod.GET, "/api/v1/organizations", permitAll)
                authorize(HttpMethod.GET, "/api/v1/organizations/{id}", permitAll)
                authorize(HttpMethod.GET, "/api/v1/users/{id}/profile", permitAll)
                authorize(HttpMethod.GET, "/api/v1/users/{id}/reputation", permitAll)
                authorize(HttpMethod.GET, "/api/v1/users/{id}/badges", permitAll)
                authorize(anyRequest, authenticated)
            }
        }
        return http.build()
    }
}
