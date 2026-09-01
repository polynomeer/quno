package com.quno.qunobackend.application.tag.usecase

import com.quno.qunobackend.application.tag.dto.TagContributorResult
import com.quno.qunobackend.application.tag.dto.toResult
import com.quno.qunobackend.domain.tag.TagNotFoundException
import com.quno.qunobackend.domain.tag.TagRepository
import com.quno.qunobackend.domain.tag.TagStatsRepository
import org.springframework.stereotype.Service

@Service
class ListTagContributorsUseCase(
    private val tagRepository: TagRepository,
    private val tagStatsRepository: TagStatsRepository,
) {
    fun execute(tagId: Long, limit: Int = 10): List<TagContributorResult> {
        tagRepository.findById(tagId) ?: throw TagNotFoundException(tagId)
        return tagStatsRepository.findTopContributors(tagId, limit).map { it.toResult() }
    }
}
