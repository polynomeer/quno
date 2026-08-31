package com.quno.qunobackend.infrastructure.persistence.jpa.repository

import com.quno.qunobackend.infrastructure.persistence.jpa.entity.TechnologyReleaseJpaEntity
import org.springframework.data.jpa.repository.JpaRepository

interface TechnologyReleaseJpaRepository : JpaRepository<TechnologyReleaseJpaEntity, Long> {
    fun findByTagSlug(tagSlug: String): TechnologyReleaseJpaEntity?
}
