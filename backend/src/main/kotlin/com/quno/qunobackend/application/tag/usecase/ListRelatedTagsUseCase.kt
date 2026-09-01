package com.quno.qunobackend.application.tag.usecase

import com.quno.qunobackend.application.tag.dto.TagResult
import com.quno.qunobackend.domain.tag.TagNotFoundException
import com.quno.qunobackend.domain.tag.TagRepository
import com.quno.qunobackend.domain.tag.TagStatsRepository
import org.springframework.stereotype.Service

@Service
class ListRelatedTagsUseCase(
    private val tagRepository: TagRepository,
    private val tagStatsRepository: TagStatsRepository,
) {
    /** Silently drops ids that no longer resolve (e.g. soft-deleted since ranking ran) — same
     * defensive pattern as QuestionSummaryHydrator. */
    fun execute(tagId: Long, limit: Int = 10): List<TagResult> {
        tagRepository.findById(tagId) ?: throw TagNotFoundException(tagId)
        return tagStatsRepository.findRelatedTagIds(tagId, limit).mapNotNull { tagRepository.findById(it)?.toResult() }
    }
}
