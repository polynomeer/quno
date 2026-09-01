package com.quno.qunobackend.application.organization.usecase

import com.quno.qunobackend.domain.organization.Organization
import com.quno.qunobackend.domain.organization.OrganizationRepository

class InMemoryOrganizationRepository : OrganizationRepository {
    private val organizationsById = mutableMapOf<Long, Organization>()
    private var nextId = 1L

    override fun findById(id: Long): Organization? = organizationsById[id]

    override fun findBySlug(slug: String): Organization? = organizationsById.values.find { it.slug == slug }

    override fun findByEmailDomain(domain: String): Organization? = organizationsById.values.find { it.emailDomain == domain }

    override fun save(organization: Organization): Organization {
        val saved = if (organization.id == null) {
            Organization.reconstitute(
                id = nextId++,
                name = organization.name,
                slug = organization.slug,
                description = organization.description,
                createdBy = organization.createdBy,
                emailDomain = organization.emailDomain,
                createdAt = organization.createdAt,
            )
        } else {
            organization
        }
        organizationsById[requireNotNull(saved.id)] = saved
        return saved
    }

    override fun search(query: String?, limit: Int): List<Organization> =
        organizationsById.values
            .filter { query.isNullOrBlank() || it.name.contains(query, ignoreCase = true) }
            .sortedBy { it.name }
            .take(limit)
}
