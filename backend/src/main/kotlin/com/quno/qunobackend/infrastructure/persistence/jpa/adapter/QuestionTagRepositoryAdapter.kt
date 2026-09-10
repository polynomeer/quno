package com.quno.qunobackend.infrastructure.persistence.jpa.adapter

import com.quno.qunobackend.domain.tag.QuestionTagRepository
import com.quno.qunobackend.domain.tag.Tag
import com.quno.qunobackend.infrastructure.persistence.jpa.entity.QuestionTagId
import com.quno.qunobackend.infrastructure.persistence.jpa.entity.QuestionTagJpaEntity
import com.quno.qunobackend.infrastructure.persistence.jpa.entity.TagJpaEntity
import com.quno.qunobackend.infrastructure.persistence.jpa.repository.QuestionTagJpaRepository
import com.quno.qunobackend.infrastructure.persistence.jpa.repository.TagJpaRepository
import org.springframework.stereotype.Component

@Component
class QuestionTagRepositoryAdapter(
    private val questionTagJpaRepository: QuestionTagJpaRepository,
    private val tagJpaRepository: TagJpaRepository,
) : QuestionTagRepository {

    override fun attach(questionId: Long, tagId: Long) {
        val id = QuestionTagId(questionId, tagId)
        if (!questionTagJpaRepository.existsById(id)) {
            questionTagJpaRepository.save(QuestionTagJpaEntity(questionId, tagId))
        }
    }

    override fun findTagsByQuestionId(questionId: Long): List<Tag> {
        val tagIds = questionTagJpaRepository.findAllByQuestionId(questionId).map { it.tagId }
        if (tagIds.isEmpty()) return emptyList()
        return tagJpaRepository.findAllById(tagIds).map { it.toDomain() }
    }

    override fun findTagsByQuestionIds(questionIds: List<Long>): Map<Long, List<Tag>> {
        if (questionIds.isEmpty()) return emptyMap()
        val links = questionTagJpaRepository.findAllByQuestionIdIn(questionIds)
        if (links.isEmpty()) return emptyMap()
        val tagsById = tagJpaRepository.findAllById(links.map { it.tagId }.distinct()).associateBy { requireNotNull(it.id) }
        return links.groupBy({ it.questionId }, { tagsById[it.tagId] })
            .mapValues { (_, tags) -> tags.filterNotNull().map { it.toDomain() } }
    }

    private fun TagJpaEntity.toDomain(): Tag = Tag.reconstitute(
        id = requireNotNull(id),
        name = name,
        slug = slug,
        description = description,
        docsUrl = docsUrl,
        deletedAt = deletedAt,
        createdAt = createdAt,
    )
}
