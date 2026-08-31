package com.quno.qunobackend.domain.organization

import java.time.Instant

/**
 * A user-created group — a study group, a company team, a community meetup (Phase 22,
 * ADR-0034). No external identity verification: unlike a real "Verified Organization" backed by
 * a company/school, this is purely self-declared, same trust level as a Tag someone creates.
 */
class Organization private constructor(
    val id: Long?,
    val name: String,
    val slug: String,
    val description: String?,
    val createdBy: Long,
    val createdAt: Instant,
) {
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
                createdAt = Instant.now(),
            )
        }

        fun reconstitute(
            id: Long,
            name: String,
            slug: String,
            description: String?,
            createdBy: Long,
            createdAt: Instant,
        ): Organization = Organization(id, name, slug, description, createdBy, createdAt)

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
