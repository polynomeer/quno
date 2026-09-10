package com.quno.qunobackend.infrastructure.persistence.jpa.adapter

import com.quno.qunobackend.domain.organization.Organization
import com.quno.qunobackend.domain.organization.OrganizationRepository
import com.quno.qunobackend.infrastructure.persistence.jpa.entity.OrganizationJpaEntity
import com.quno.qunobackend.infrastructure.persistence.jpa.repository.OrganizationJpaRepository
import org.springframework.data.domain.PageRequest
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.stereotype.Component
import tools.jackson.databind.ObjectMapper
import java.time.Duration
import java.time.Instant

@Component
class OrganizationRepositoryAdapter(
    private val jpaRepository: OrganizationJpaRepository,
    private val redisTemplate: StringRedisTemplate,
    private val objectMapper: ObjectMapper,
) : OrganizationRepository {

    override fun findById(id: Long): Organization? = jpaRepository.findById(id).orElse(null)?.toDomain()

    override fun findAllByIds(ids: List<Long>): List<Organization> {
        if (ids.isEmpty()) return emptyList()
        return jpaRepository.findAllById(ids).map { it.toDomain() }
    }

    override fun findBySlug(slug: String): Organization? = jpaRepository.findBySlug(slug)?.toDomain()

    override fun findByEmailDomain(domain: String): Organization? = jpaRepository.findByEmailDomain(domain)?.toDomain()

    override fun save(organization: Organization): Organization {
        val entity = OrganizationJpaEntity(
            id = organization.id,
            name = organization.name,
            slug = organization.slug,
            description = organization.description,
            createdBy = organization.createdBy,
            emailDomain = organization.emailDomain,
            createdAt = organization.createdAt,
        )
        return jpaRepository.save(entity).toDomain()
    }

    /**
     * Cache-aside, 같은 패턴을 DashboardRepositoryAdapter가 이미 쓰고 있다(popular
     * questions/trending tags). 조직은 생성이 드물고(태그 설명과 달리 위키 편집 대상도 아님)
     * `GET /organizations`가 공개 엔드포인트(ADR-0042)라 캐싱 이득이 실제 트래픽에 반영된다
     * (quality-improvement-plan.md Q-2). 도메인 객체(`Organization`)는 private 생성자라
     * Jackson이 바로 역직렬화할 수 없어, [CachedOrganization]이라는 평범한 데이터 클래스를
     * 캐시에 넣고 읽을 때 `reconstitute`로 되돌린다.
     */
    override fun search(query: String?, limit: Int): List<Organization> {
        val key = "$SEARCH_CACHE_KEY:${query.orEmpty()}:$limit"
        redisTemplate.opsForValue().get(key)?.let { cached ->
            return objectMapper.readValue(cached, Array<CachedOrganization>::class.java).map { it.toDomain() }
        }

        val pageable = PageRequest.of(0, limit)
        val entities = if (query.isNullOrBlank()) {
            jpaRepository.findAllByOrderByNameAsc(pageable)
        } else {
            jpaRepository.findAllByNameContainingIgnoreCaseOrderByNameAsc(query, pageable)
        }
        val result = entities.map { it.toDomain() }

        val cacheable = result.map { CachedOrganization(it.id!!, it.name, it.slug, it.description, it.createdBy, it.emailDomain, it.createdAt) }
        redisTemplate.opsForValue().set(key, objectMapper.writeValueAsString(cacheable), CACHE_TTL)
        return result
    }

    private fun OrganizationJpaEntity.toDomain(): Organization = Organization.reconstitute(
        id = requireNotNull(id),
        name = name,
        slug = slug,
        description = description,
        createdBy = createdBy,
        emailDomain = emailDomain,
        createdAt = createdAt,
    )

    companion object {
        private const val SEARCH_CACHE_KEY = "organizations:search"
        private val CACHE_TTL = Duration.ofSeconds(60)
    }
}

private data class CachedOrganization(
    val id: Long,
    val name: String,
    val slug: String,
    val description: String?,
    val createdBy: Long,
    val emailDomain: String?,
    val createdAt: Instant,
) {
    fun toDomain(): Organization = Organization.reconstitute(id, name, slug, description, createdBy, emailDomain, createdAt)
}
