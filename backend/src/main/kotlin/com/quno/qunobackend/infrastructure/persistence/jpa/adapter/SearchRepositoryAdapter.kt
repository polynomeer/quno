package com.quno.qunobackend.infrastructure.persistence.jpa.adapter

import com.quno.qunobackend.domain.question.QuestionViewer
import com.quno.qunobackend.domain.search.SearchRepository
import com.quno.qunobackend.domain.search.SearchSort
import com.quno.qunobackend.infrastructure.persistence.jpa.repository.SearchJpaRepository
import org.springframework.stereotype.Component

@Component
class SearchRepositoryAdapter(
    private val searchJpaRepository: SearchJpaRepository,
) : SearchRepository {

    override fun searchQuestionIds(query: String, limit: Int, sort: SearchSort, viewer: QuestionViewer): List<Long> = when (sort) {
        SearchSort.RELEVANCE -> searchJpaRepository.searchQuestionIdsByRelevance(query, limit, viewer.userIdForSql(), viewer.organizationIdsForSql())
        SearchSort.SCORE -> searchJpaRepository.searchQuestionIdsByScore(query, limit, viewer.userIdForSql(), viewer.organizationIdsForSql())
    }

    override fun findRelatedQuestionIds(questionId: Long, limit: Int, viewer: QuestionViewer): List<Long> = searchJpaRepository.findRelatedQuestionIds(questionId, limit, viewer.userIdForSql(), viewer.organizationIdsForSql())
}
