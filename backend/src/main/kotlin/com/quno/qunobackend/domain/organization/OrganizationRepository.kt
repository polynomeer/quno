package com.quno.qunobackend.domain.organization

/** Port implemented by infrastructure/persistence/jpa/adapter/OrganizationRepositoryAdapter. */
interface OrganizationRepository {
    fun findById(id: Long): Organization?

    /** Batch form of [findById] — avoids N+1 when hydrating a list of organization ids (a
     * user's memberships etc.). Order is not significant; caller re-orders by id. */
    fun findAllByIds(ids: List<Long>): List<Organization>
    fun findBySlug(slug: String): Organization?

    /** Phase 23 — the one Organization already verified for this domain, if any. */
    fun findByEmailDomain(domain: String): Organization?
    fun save(organization: Organization): Organization

    /** Name-matching [query] (case-insensitive) when given, ordered by name — same shape as TagRepository.search. */
    fun search(query: String?, limit: Int): List<Organization>
}
