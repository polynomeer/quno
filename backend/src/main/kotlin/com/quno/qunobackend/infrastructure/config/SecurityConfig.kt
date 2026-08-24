package com.quno.qunobackend.infrastructure.config

import com.quno.qunobackend.application.user.TokenProvider
import com.quno.qunobackend.infrastructure.security.JwtAuthenticationFilter
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpStatus
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.invoke
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.authentication.HttpStatusEntryPoint

/** Baseline chain for a stateless, JWT-authenticated API (see docs/architecture/api-design.md). */
@Configuration
class SecurityConfig(
    private val tokenProvider: TokenProvider,
) {

    @Bean
    fun passwordEncoder(): PasswordEncoder = BCryptPasswordEncoder()

    @Bean
    fun filterChain(http: HttpSecurity): SecurityFilterChain {
        http.addFilterBefore(JwtAuthenticationFilter(tokenProvider), UsernamePasswordAuthenticationFilter::class.java)
        http {
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
                authorize(anyRequest, authenticated)
            }
        }
        return http.build()
    }
}
