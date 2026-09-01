package com.quno.qunobackend.domain.tag

import java.time.Instant

class Tag private constructor(
    val id: Long?,
    val name: String,
    val slug: String,
    /** Wiki-style, like the tag itself — any authenticated user can edit (Phase 28, ADR-0040),
     * same trust level as creating the tag in the first place. */
    val description: String?,
    val docsUrl: String?,
    val deletedAt: Instant?,
    val createdAt: Instant,
) {
    fun rename(newName: String): Tag {
        require(newName.isNotBlank()) { "name must not be blank" }
        val normalized = newName.trim()
        return Tag(id, normalized, slugify(normalized), description, docsUrl, deletedAt, createdAt)
    }

    fun updateDetails(description: String?, docsUrl: String?): Tag =
        Tag(id, name, slug, description?.trim()?.ifBlank { null }, docsUrl?.trim()?.ifBlank { null }, deletedAt, createdAt)

    fun softDelete(): Tag = Tag(id, name, slug, description, docsUrl, Instant.now(), createdAt)

    companion object {
        fun create(name: String): Tag {
            require(name.isNotBlank()) { "name must not be blank" }
            val normalized = name.trim()
            return Tag(
                id = null, name = normalized, slug = slugify(normalized),
                description = null, docsUrl = null, deletedAt = null, createdAt = Instant.now(),
            )
        }

        fun reconstitute(
            id: Long,
            name: String,
            slug: String,
            description: String?,
            docsUrl: String?,
            deletedAt: Instant?,
            createdAt: Instant,
        ): Tag = Tag(id, name, slug, description, docsUrl, deletedAt, createdAt)

        /**
         * Exposed so callers can look up an existing tag by the slug a candidate name would
         * produce *before* inserting — "Kotlin" and "kotlin" collide on slug (uq_tags_slug_active)
         * even though their exact `name` differs, so find-or-create must key off this, not `name`.
         */
        fun slugify(name: String): String = name.lowercase()
            .replace(Regex("[^a-z0-9\\s-]"), "")
            .trim()
            .replace(Regex("\\s+"), "-")
    }
}
