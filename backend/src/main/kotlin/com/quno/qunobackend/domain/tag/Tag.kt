package com.quno.qunobackend.domain.tag

import java.time.Instant

class Tag private constructor(
    val id: Long?,
    val name: String,
    val slug: String,
    val deletedAt: Instant?,
    val createdAt: Instant,
) {
    fun rename(newName: String): Tag {
        require(newName.isNotBlank()) { "name must not be blank" }
        val normalized = newName.trim()
        return Tag(id, normalized, slugify(normalized), deletedAt, createdAt)
    }

    fun softDelete(): Tag = Tag(id, name, slug, Instant.now(), createdAt)

    companion object {
        fun create(name: String): Tag {
            require(name.isNotBlank()) { "name must not be blank" }
            val normalized = name.trim()
            return Tag(id = null, name = normalized, slug = slugify(normalized), deletedAt = null, createdAt = Instant.now())
        }

        fun reconstitute(id: Long, name: String, slug: String, deletedAt: Instant?, createdAt: Instant): Tag =
            Tag(id, name, slug, deletedAt, createdAt)

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
