package com.quno.qunobackend.application.search.usecase

import com.quno.qunobackend.application.common.QuestionSummaryHydrator
import com.quno.qunobackend.application.search.dto.QuestionSearchResult
import com.quno.qunobackend.domain.question.QuestionNotFoundException
import com.quno.qunobackend.domain.question.QuestionRepository
import com.quno.qunobackend.domain.search.SearchRepository
import com.quno.qunobackend.domain.search.SearchSort
import org.springframework.stereotype.Service

/**
 * Both operations here rank candidate question ids (lexical match or tag overlap) and then
 * hydrate them into summaries — kept as one class since they share every dependency and the
 * hydration step, and are really two ranking strategies over the same "related questions"
 * concept (see docs/product/mvp-scope.md P1 "유사 질문 추천").
 */
@Service
class QuestionSearchUseCase(
    private val searchRepository: SearchRepository,
    private val questionRepository: QuestionRepository,
    private val hydrator: QuestionSummaryHydrator,
) {
    fun search(query: String, limit: Int = 20, sort: SearchSort = SearchSort.RELEVANCE): List<QuestionSearchResult> =
        hydrator.hydrate(searchRepository.searchQuestionIds(query, limit, sort))

    fun related(questionId: Long, limit: Int = 5): List<QuestionSearchResult> {
        questionRepository.findById(questionId) ?: throw QuestionNotFoundException(questionId)
        return hydrator.hydrate(searchRepository.findRelatedQuestionIds(questionId, limit))
    }
}
