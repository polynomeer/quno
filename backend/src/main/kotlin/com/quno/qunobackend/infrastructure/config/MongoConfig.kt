package com.quno.qunobackend.infrastructure.config

import org.springframework.boot.mongodb.autoconfigure.MongoClientSettingsBuilderCustomizer
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.util.concurrent.TimeUnit

/** MongoDB 드라이버 기본 serverSelectionTimeout(30초)이면 Mongo 장애 시 Live Chat 메시지
 * 조회가 30초씩 멈춘 뒤에야 실패한다(장애 시나리오 A3, ADR-0055) — HikariCP connection-timeout을
 * 3초로 낮춘 것(ADR-0054)과 같은 이유로 3초로 낮춘다. */
@Configuration
class MongoConfig {

    @Bean
    fun mongoClientSettingsBuilderCustomizer(): MongoClientSettingsBuilderCustomizer =
        MongoClientSettingsBuilderCustomizer { builder ->
            builder.applyToClusterSettings {
                it.serverSelectionTimeout(3, TimeUnit.SECONDS)
            }
        }
}
