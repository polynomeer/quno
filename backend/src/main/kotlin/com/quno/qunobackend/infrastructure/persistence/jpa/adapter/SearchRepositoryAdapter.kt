package com.quno.qunobackend.infrastructure.persistence.jpa.adapter

import com.quno.qunobackend.domain.search.SearchRepository
import com.quno.qunobackend.domain.search.SearchSort
import com.quno.qunobackend.infrastructure.persistence.jpa.repository.SearchJpaRepository
import org.springframework.stereotype.Component

@Component
class SearchRepositoryAdapter(
    private val searchJpaRepository: SearchJpaRepository,
) : SearchRepository {

    override fun searchQuestionIds(query: String, limit: Int, sort: SearchSort): List<Long> = when (sort) {
        SearchSort.RELEVANCE -> searchJpaRepository.searchQuestionIdsByRelevance(query, limit)
        SearchSort.SCORE -> searchJpaRepository.searchQuestionIdsByScore(query, limit)
    }

    override fun findRelatedQuestionIds(questionId: Long, limit: Int): List<Long> =
        searchJpaRepository.findRelatedQuestionIds(questionId, limit)
}
