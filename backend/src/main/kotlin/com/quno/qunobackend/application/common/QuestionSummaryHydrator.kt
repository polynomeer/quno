package com.quno.qunobackend.application.common

import com.quno.qunobackend.application.search.dto.QuestionSearchResult
import com.quno.qunobackend.domain.question.QuestionRepository
import com.quno.qunobackend.domain.tag.QuestionTagRepository
import org.springframework.stereotype.Component

/**
 * Turns ranked question ids (from Search, Related, or Recommendation) into display summaries.
 * Shared because search/related/recommend are all "rank candidate ids, then hydrate" — only
 * the ranking strategy differs between them.
 */
@Component
class QuestionSummaryHydrator(
    private val questionRepository: QuestionRepository,
    private val questionTagRepository: QuestionTagRepository,
) {
    /** Silently drops ids that no longer resolve to a question (e.g. deleted since ranking ran). */
    fun hydrate(ids: List<Long>): List<QuestionSearchResult> = ids.mapNotNull(::toSummary)

    private fun toSummary(id: Long): QuestionSearchResult? {
        val question = questionRepository.findById(id) ?: return null
        val tags = questionTagRepository.findTagsByQuestionId(id).map { it.name }
        return QuestionSearchResult(id = requireNotNull(question.id), title = question.title, status = question.status, tags = tags)
    }
}
