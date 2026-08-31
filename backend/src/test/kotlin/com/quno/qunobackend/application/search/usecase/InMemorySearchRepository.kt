package com.quno.qunobackend.application.search.usecase

import com.quno.qunobackend.domain.search.SearchRepository
import com.quno.qunobackend.domain.search.SearchSort

class InMemorySearchRepository : SearchRepository {
    var searchResults: List<Long> = emptyList()
    var relatedResults: Map<Long, List<Long>> = emptyMap()

    /** Records the last `sort` this fake was called with — lets tests assert the use case
     * actually passes it through, without re-implementing the real SQL ranking (Phase 20). */
    var lastSort: SearchSort? = null

    override fun searchQuestionIds(query: String, limit: Int, sort: SearchSort): List<Long> {
        lastSort = sort
        return searchResults.take(limit)
    }

    override fun findRelatedQuestionIds(questionId: Long, limit: Int): List<Long> =
        (relatedResults[questionId] ?: emptyList()).take(limit)
}
