package com.quno.qunobackend.domain.search

/**
 * Port implemented by infrastructure/persistence/jpa/adapter/SearchRepositoryAdapter.
 * MVP uses PostgreSQL full-text search + tag matching (see docs/architecture/api-design.md);
 * a dedicated search engine is a later-stage upgrade per docs/architecture/system-architecture.md.
 */
interface SearchRepository {
    /** Matches the latest version's title/body/logs (lexical) or a tag name; most relevant first. */
    fun searchQuestionIds(query: String, limit: Int): List<Long>

    /** Tag-overlap ranked, excluding the question itself; most shared tags first. */
    fun findRelatedQuestionIds(questionId: Long, limit: Int): List<Long>
}
