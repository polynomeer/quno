package com.quno.qunobackend.domain.organization

import java.time.Instant

/**
 * A user-created group — a study group, a company team, a community meetup (Phase 22,
 * ADR-0034), or a real company/school confirmed via [emailDomain] (Phase 23, ADR-0035).
 * [emailDomain] is null for Virtual/Community organizations (self-declared, same trust level as
 * a Tag someone creates) and set only through email domain verification — never through
 * [create], which is the only path a user directly controls.
 */
class Organization private constructor(
    val id: Long?,
    val name: String,
    val slug: String,
    val description: String?,
    val createdBy: Long,
    val emailDomain: String?,
    val createdAt: Instant,
) {
    val verified: Boolean get() = emailDomain != null

    /** Upgrades a pre-existing Virtual/Community organization that happens to share a verified
     * domain's name (e.g. someone created "google.com" before anyone verified that domain) —
     * see ADR-0035's note on this edge case. */
    fun verify(emailDomain: String): Organization {
        check(!verified) { "organization is already verified" }
        return Organization(id, name, slug, description, createdBy, emailDomain, createdAt)
    }

    companion object {
        fun create(name: String, description: String?, createdBy: Long): Organization {
            require(name.isNotBlank()) { "name must not be blank" }
            val normalized = name.trim()
            return Organization(
                id = null,
                name = normalized,
                slug = slugify(normalized),
                description = description?.trim()?.ifBlank { null },
                createdBy = createdBy,
                emailDomain = null,
                createdAt = Instant.now(),
            )
        }

        /** The only way an organization is born already-verified — used by
         * ConfirmEmailDomainVerificationUseCase's find-or-create, never by [CreateOrganizationUseCase]. */
        fun verifiedFor(domain: String, createdBy: Long): Organization = Organization(
            id = null,
            name = domain,
            slug = domain,
            description = null,
            createdBy = createdBy,
            emailDomain = domain,
            createdAt = Instant.now(),
        )

        fun reconstitute(
            id: Long,
            name: String,
            slug: String,
            description: String?,
            createdBy: Long,
            emailDomain: String?,
            createdAt: Instant,
        ): Organization = Organization(id, name, slug, description, createdBy, emailDomain, createdAt)

        /**
         * Unlike [com.quno.qunobackend.domain.tag.Tag.slugify], this only lowercases + trims —
         * it does not strip non-ASCII characters. Organization names are frequently Korean
         * (e.g. "대구 백엔드 개발자 모임", see docs/archive), and Tag's ASCII-only slugify would
         * collapse every such name to an empty string. This slug is never used in a URL path
         * (organizations are addressed by id), so it only needs to catch case-only duplicates,
         * not be URL-safe.
         */
        fun slugify(name: String): String = name.trim().lowercase()
    }
}
