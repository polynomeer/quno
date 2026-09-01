package com.quno.qunobackend.application.tag.usecase

import com.quno.qunobackend.application.common.QuestionSummaryHydrator
import com.quno.qunobackend.application.search.dto.QuestionSearchResult
import com.quno.qunobackend.domain.tag.TagNotFoundException
import com.quno.qunobackend.domain.tag.TagQuestionSort
import com.quno.qunobackend.domain.tag.TagRepository
import com.quno.qunobackend.domain.tag.TagStatsRepository
import org.springframework.stereotype.Service

/** Replaces the ADR-0021 search-approximation Tag Detail used — ranks candidate ids directly off
 * question_tags instead of piggybacking on full-text search (Phase 28, ADR-0040). */
@Service
class ListTagQuestionsUseCase(
    private val tagRepository: TagRepository,
    private val tagStatsRepository: TagStatsRepository,
    private val hydrator: QuestionSummaryHydrator,
) {
    fun execute(tagId: Long, sort: TagQuestionSort, limit: Int = 20): List<QuestionSearchResult> {
        tagRepository.findById(tagId) ?: throw TagNotFoundException(tagId)
        return hydrator.hydrate(tagStatsRepository.findQuestionIds(tagId, sort, limit))
    }
}
