package com.quno.qunobackend.infrastructure.config

import org.slf4j.LoggerFactory
import org.springframework.cache.Cache
import org.springframework.cache.CacheManager
import org.springframework.cache.annotation.CachingConfigurer
import org.springframework.cache.annotation.EnableCaching
import org.springframework.cache.interceptor.CacheErrorHandler
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.redis.cache.RedisCacheConfiguration
import org.springframework.data.redis.cache.RedisCacheManager
import org.springframework.data.redis.connection.RedisConnectionFactory
import org.springframework.data.redis.serializer.GenericJacksonJsonRedisSerializer
import org.springframework.data.redis.serializer.RedisSerializationContext
import tools.jackson.databind.ObjectMapper
import java.time.Duration

/**
 * `@Cacheable`/`@EnableCaching` 뒤에 붙는 [CacheManager]를 Spring Boot의 Redis 캐시
 * 자동설정(`spring-boot-starter-cache`) 없이 직접 구성한다 — 이 프로젝트는 Jackson 3(`tools.jackson`)를
 * 쓰는데, classpath에 `spring-data-redis`가 끌어오는 Jackson 2도 함께 있어 자동설정이 어느
 * `ObjectMapper`를 골라 직렬화기를 만들지 확신할 수 없었다(docs/engineering/backend-architecture-improvements.md
 * 2절). 자동설정에 맡기는 대신 앱이 이미 쓰는 [ObjectMapper] 빈을 [GenericJacksonJsonRedisSerializer]에
 * 직접 넘겨 모호함을 원천적으로 없앤다.
 *
 * `errorHandler()`는 Redis 장애 시 캐시 get/put을 조용한 미스/무시로 처리한다 — 어댑터마다
 * `safeCacheGet`/`safeCacheSet`을 기억해서 감싸야 했던 ADR-0056의 정책을 `@Cacheable`을 쓰는
 * 모든 캐시에 자동으로 적용한다.
 */
@Configuration
@EnableCaching
class CacheConfig(
    private val objectMapper: ObjectMapper,
) : CachingConfigurer {

    @Bean
    fun cacheManager(connectionFactory: RedisConnectionFactory): CacheManager {
        val defaultConfig = RedisCacheConfiguration.defaultCacheConfig()
            .entryTtl(Duration.ofSeconds(60))
            .serializeValuesWith(
                RedisSerializationContext.SerializationPair.fromSerializer(GenericJacksonJsonRedisSerializer(objectMapper)),
            )
        return RedisCacheManager.builder(connectionFactory)
            .cacheDefaults(defaultConfig)
            .build()
    }

    override fun errorHandler(): CacheErrorHandler = LoggingCacheErrorHandler()
}

private class LoggingCacheErrorHandler : CacheErrorHandler {
    private val logger = LoggerFactory.getLogger(LoggingCacheErrorHandler::class.java)

    override fun handleCacheGetError(exception: RuntimeException, cache: Cache, key: Any) {
        logger.warn("Redis unavailable, skipping cache read for cache={} key={}", cache.name, key, exception)
    }

    override fun handleCachePutError(exception: RuntimeException, cache: Cache, key: Any, value: Any?) {
        logger.warn("Redis unavailable, skipping cache write for cache={} key={}", cache.name, key, exception)
    }

    override fun handleCacheEvictError(exception: RuntimeException, cache: Cache, key: Any) {
        logger.warn("Redis unavailable, skipping cache evict for cache={} key={}", cache.name, key, exception)
    }

    override fun handleCacheClearError(exception: RuntimeException, cache: Cache) {
        logger.warn("Redis unavailable, skipping cache clear for cache={}", cache.name, exception)
    }
}
