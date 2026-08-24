package com.quno.qunobackend.application.search.usecase

import com.quno.qunobackend.domain.search.SearchRepository

class InMemorySearchRepository : SearchRepository {
    var searchResults: List<Long> = emptyList()
    var relatedResults: Map<Long, List<Long>> = emptyMap()

    override fun searchQuestionIds(query: String, limit: Int): List<Long> = searchResults.take(limit)

    override fun findRelatedQuestionIds(questionId: Long, limit: Int): List<Long> =
        (relatedResults[questionId] ?: emptyList()).take(limit)
}
