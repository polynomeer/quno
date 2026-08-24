package com.quno.qunobackend.infrastructure.persistence.jpa.adapter

import com.quno.qunobackend.domain.search.SearchRepository
import com.quno.qunobackend.infrastructure.persistence.jpa.repository.SearchJpaRepository
import org.springframework.stereotype.Component

@Component
class SearchRepositoryAdapter(
    private val searchJpaRepository: SearchJpaRepository,
) : SearchRepository {

    override fun searchQuestionIds(query: String, limit: Int): List<Long> =
        searchJpaRepository.searchQuestionIds(query, limit)

    override fun findRelatedQuestionIds(questionId: Long, limit: Int): List<Long> =
        searchJpaRepository.findRelatedQuestionIds(questionId, limit)
}
