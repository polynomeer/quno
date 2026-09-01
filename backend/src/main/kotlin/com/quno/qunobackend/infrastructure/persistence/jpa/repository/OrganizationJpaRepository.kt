package com.quno.qunobackend.infrastructure.persistence.jpa.repository

import com.quno.qunobackend.infrastructure.persistence.jpa.entity.OrganizationJpaEntity
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository

interface OrganizationJpaRepository : JpaRepository<OrganizationJpaEntity, Long> {
    fun findBySlug(slug: String): OrganizationJpaEntity?
    fun findByEmailDomain(emailDomain: String): OrganizationJpaEntity?
    fun findAllByOrderByNameAsc(pageable: Pageable): List<OrganizationJpaEntity>
    fun findAllByNameContainingIgnoreCaseOrderByNameAsc(name: String, pageable: Pageable): List<OrganizationJpaEntity>
}
