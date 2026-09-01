package com.quno.qunobackend.application.tag.usecase

import com.quno.qunobackend.application.tag.dto.TagResult
import com.quno.qunobackend.domain.tag.TagNotFoundException
import com.quno.qunobackend.domain.tag.TagRepository
import org.springframework.stereotype.Service

/** Wiki-style edit, open to any authenticated user (Phase 28, ADR-0040) — same trust level as
 * creating the tag itself (Tag.slugify's find-or-create has no ownership either). */
@Service
class UpdateTagDetailsUseCase(
    private val tagRepository: TagRepository,
) {
    fun execute(id: Long, description: String?, docsUrl: String?): TagResult {
        val tag = tagRepository.findById(id) ?: throw TagNotFoundException(id)
        return tagRepository.save(tag.updateDetails(description, docsUrl)).toResult()
    }
}
