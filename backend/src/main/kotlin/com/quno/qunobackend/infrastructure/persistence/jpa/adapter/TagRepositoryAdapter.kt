package com.quno.qunobackend.infrastructure.persistence.jpa.adapter

import com.quno.qunobackend.domain.tag.Tag
import com.quno.qunobackend.domain.tag.TagRepository
import com.quno.qunobackend.infrastructure.persistence.jpa.entity.TagJpaEntity
import com.quno.qunobackend.infrastructure.persistence.jpa.repository.TagJpaRepository
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Component

@Component
class TagRepositoryAdapter(
    private val jpaRepository: TagJpaRepository,
) : TagRepository {

    override fun save(tag: Tag): Tag {
        val entity = TagJpaEntity(
            id = tag.id,
            name = tag.name,
            slug = tag.slug,
            description = tag.description,
            docsUrl = tag.docsUrl,
            deletedAt = tag.deletedAt,
            createdAt = tag.createdAt,
        )
        return jpaRepository.save(entity).toDomain()
    }

    override fun findById(id: Long): Tag? = jpaRepository.findByIdAndDeletedAtIsNull(id)?.toDomain()

    override fun findBySlug(slug: String): Tag? = jpaRepository.findBySlugAndDeletedAtIsNull(slug)?.toDomain()

    override fun search(query: String?, limit: Int): List<Tag> {
        val pageable = PageRequest.of(0, limit)
        val entities = if (query.isNullOrBlank()) {
            jpaRepository.findAllByDeletedAtIsNullOrderByNameAsc(pageable)
        } else {
            jpaRepository.findAllByDeletedAtIsNullAndNameContainingIgnoreCaseOrderByNameAsc(query, pageable)
        }
        return entities.map { it.toDomain() }
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
