package com.quno.qunobackend.infrastructure.persistence.jpa.adapter

import com.quno.qunobackend.domain.tag.Tag
import com.quno.qunobackend.domain.tag.QuestionTagRepository
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

    private fun TagJpaEntity.toDomain(): Tag = Tag.reconstitute(
        id = requireNotNull(id),
        name = name,
        slug = slug,
        deletedAt = deletedAt,
        createdAt = createdAt,
    )
}
