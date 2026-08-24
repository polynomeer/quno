package com.quno.qunobackend.infrastructure.persistence.jpa.repository

import com.quno.qunobackend.infrastructure.persistence.jpa.entity.TagJpaEntity
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository

interface TagJpaRepository : JpaRepository<TagJpaEntity, Long> {
    fun findByIdAndDeletedAtIsNull(id: Long): TagJpaEntity?
    fun findBySlugAndDeletedAtIsNull(slug: String): TagJpaEntity?
    fun findAllByDeletedAtIsNullOrderByNameAsc(pageable: Pageable): List<TagJpaEntity>
    fun findAllByDeletedAtIsNullAndNameContainingIgnoreCaseOrderByNameAsc(name: String, pageable: Pageable): List<TagJpaEntity>
}
