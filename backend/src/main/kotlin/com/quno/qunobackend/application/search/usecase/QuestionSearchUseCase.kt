package com.quno.qunobackend.application.search.usecase

import com.quno.qunobackend.application.search.dto.QuestionSearchResult
import com.quno.qunobackend.domain.question.QuestionNotFoundException
import com.quno.qunobackend.domain.question.QuestionRepository
import com.quno.qunobackend.domain.search.SearchRepository
import com.quno.qunobackend.domain.tag.QuestionTagRepository
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
    private val questionTagRepository: QuestionTagRepository,
) {
    fun search(query: String, limit: Int = 20): List<QuestionSearchResult> =
        searchRepository.searchQuestionIds(query, limit).mapNotNull(::toSummary)

    fun related(questionId: Long, limit: Int = 5): List<QuestionSearchResult> {
        questionRepository.findById(questionId) ?: throw QuestionNotFoundException(questionId)
        return searchRepository.findRelatedQuestionIds(questionId, limit).mapNotNull(::toSummary)
    }

    private fun toSummary(id: Long): QuestionSearchResult? {
        val question = questionRepository.findById(id) ?: return null
        val tags = questionTagRepository.findTagsByQuestionId(id).map { it.name }
        return QuestionSearchResult(id = requireNotNull(question.id), title = question.title, status = question.status, tags = tags)
    }
}
