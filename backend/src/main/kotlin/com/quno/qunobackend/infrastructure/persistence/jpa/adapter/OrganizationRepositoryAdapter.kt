package com.quno.qunobackend.infrastructure.persistence.jpa.adapter

import com.quno.qunobackend.domain.organization.Organization
import com.quno.qunobackend.domain.organization.OrganizationRepository
import com.quno.qunobackend.infrastructure.persistence.jpa.entity.OrganizationJpaEntity
import com.quno.qunobackend.infrastructure.persistence.jpa.repository.OrganizationJpaRepository
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Component

@Component
class OrganizationRepositoryAdapter(
    private val jpaRepository: OrganizationJpaRepository,
) : OrganizationRepository {

    override fun findById(id: Long): Organization? = jpaRepository.findById(id).orElse(null)?.toDomain()

    override fun findBySlug(slug: String): Organization? = jpaRepository.findBySlug(slug)?.toDomain()

    override fun save(organization: Organization): Organization {
        val entity = OrganizationJpaEntity(
            id = organization.id,
            name = organization.name,
            slug = organization.slug,
            description = organization.description,
            createdBy = organization.createdBy,
            createdAt = organization.createdAt,
        )
        return jpaRepository.save(entity).toDomain()
    }

    override fun search(query: String?, limit: Int): List<Organization> {
        val pageable = PageRequest.of(0, limit)
        val entities = if (query.isNullOrBlank()) {
            jpaRepository.findAllByOrderByNameAsc(pageable)
        } else {
            jpaRepository.findAllByNameContainingIgnoreCaseOrderByNameAsc(query, pageable)
        }
        return entities.map { it.toDomain() }
    }

    private fun OrganizationJpaEntity.toDomain(): Organization = Organization.reconstitute(
        id = requireNotNull(id),
        name = name,
        slug = slug,
        description = description,
        createdBy = createdBy,
        createdAt = createdAt,
    )
}
