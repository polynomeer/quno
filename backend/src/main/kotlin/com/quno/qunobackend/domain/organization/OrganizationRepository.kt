package com.quno.qunobackend.domain.organization

/** Port implemented by infrastructure/persistence/jpa/adapter/OrganizationRepositoryAdapter. */
interface OrganizationRepository {
    fun findById(id: Long): Organization?
    fun findBySlug(slug: String): Organization?

    /** Phase 23 — the one Organization already verified for this domain, if any. */
    fun findByEmailDomain(domain: String): Organization?
    fun save(organization: Organization): Organization

    /** Name-matching [query] (case-insensitive) when given, ordered by name — same shape as TagRepository.search. */
    fun search(query: String?, limit: Int): List<Organization>
}
