package com.quno.qunobackend.application.tag.usecase

import com.quno.qunobackend.application.tag.dto.TagResult
import com.quno.qunobackend.domain.tag.TagRepository
import org.springframework.stereotype.Service

@Service
class SearchTagsUseCase(
    private val tagRepository: TagRepository,
) {
    fun execute(query: String?, limit: Int = 20): List<TagResult> =
        tagRepository.search(query, limit).map { it.toResult() }
}
