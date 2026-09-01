package com.quno.qunobackend.application.tag.usecase

import com.quno.qunobackend.application.tag.dto.TagResult
import com.quno.qunobackend.domain.tag.Tag
import com.quno.qunobackend.domain.tag.TagNotFoundException
import com.quno.qunobackend.domain.tag.TagRepository
import org.springframework.stereotype.Service

/** Tag Detail's header (Phase 28, ADR-0040) — there was previously no "look up one tag" endpoint,
 * only search. */
@Service
class GetTagUseCase(
    private val tagRepository: TagRepository,
) {
    fun execute(id: Long): TagResult = (tagRepository.findById(id) ?: throw TagNotFoundException(id)).toResult()
}

internal fun Tag.toResult() = TagResult(id = requireNotNull(id), name = name, slug = slug, description = description, docsUrl = docsUrl)
