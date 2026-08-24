package com.quno.qunobackend.infrastructure.config

import com.quno.qunobackend.infrastructure.security.JwtProperties
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Configuration

@Configuration
@EnableConfigurationProperties(JwtProperties::class)
class JwtConfig
