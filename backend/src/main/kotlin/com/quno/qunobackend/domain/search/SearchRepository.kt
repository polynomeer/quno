package com.quno.qunobackend.domain.search

import com.quno.qunobackend.domain.question.QuestionViewer

/**
 * Port implemented by infrastructure/persistence/jpa/adapter/SearchRepositoryAdapter.
 * MVP uses PostgreSQL full-text search + tag matching (see docs/architecture/api-design.md);
 * a dedicated search engine is a later-stage upgrade per docs/architecture/system-architecture.md.
 */
interface SearchRepository {
    /** Matches the latest version's title/body/logs (lexical) or a tag name. [sort] only changes
     * the final ordering — the matched candidate set is identical either way (Phase 20, ADR-0032). */
    fun searchQuestionIds(query: String, limit: Int, sort: SearchSort = SearchSort.RELEVANCE, viewer: QuestionViewer): List<Long>

    /** Tag-overlap ranked, excluding the question itself; most shared tags first. Both only
     * return questions [viewer] may see (Phase 79, ADR-0085). */
    fun findRelatedQuestionIds(questionId: Long, limit: Int, viewer: QuestionViewer): List<Long>
}
