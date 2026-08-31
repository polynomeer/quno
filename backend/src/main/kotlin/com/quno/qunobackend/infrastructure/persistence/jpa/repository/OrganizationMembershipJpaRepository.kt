package com.quno.qunobackend.infrastructure.persistence.jpa.repository

import com.quno.qunobackend.infrastructure.persistence.jpa.entity.OrganizationMembershipId
import com.quno.qunobackend.infrastructure.persistence.jpa.entity.OrganizationMembershipJpaEntity
import org.springframework.data.jpa.repository.JpaRepository

interface OrganizationMembershipJpaRepository : JpaRepository<OrganizationMembershipJpaEntity, OrganizationMembershipId> {
    fun countByOrganizationId(organizationId: Long): Long
    fun findAllByUserId(userId: Long): List<OrganizationMembershipJpaEntity>
}
