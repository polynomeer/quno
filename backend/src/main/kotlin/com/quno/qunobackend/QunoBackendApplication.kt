package com.quno.qunobackend

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.boot.security.autoconfigure.UserDetailsServiceAutoConfiguration

// Authentication is JWT-based (see infrastructure/security), not the default in-memory UserDetailsService.
@SpringBootApplication(exclude = [UserDetailsServiceAutoConfiguration::class])
class QunoBackendApplication

fun main(args: Array<String>) {
    runApplication<QunoBackendApplication>(*args)
}
