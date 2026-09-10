package com.quno.qunobackend.domain.tag

/** Port implemented by infrastructure/persistence/jpa/adapter/TagRepositoryAdapter. */
interface TagRepository {
    fun save(tag: Tag): Tag

    /** Excludes soft-deleted tags. */
    fun findById(id: Long): Tag?

    /** Batch form of [findById] — avoids N+1 when hydrating a list of tag ids (followed tags
     * etc.). Order is not significant; caller re-orders by id. */
    fun findAllByIds(ids: List<Long>): List<Tag>

    /** Slug equality, not exact name — see Tag.slugify's kdoc. */
    fun findBySlug(slug: String): Tag?

    /** Active tags, name-matching [query] (case-insensitive) when given, ordered by name. */
    fun search(query: String?, limit: Int): List<Tag>
}
