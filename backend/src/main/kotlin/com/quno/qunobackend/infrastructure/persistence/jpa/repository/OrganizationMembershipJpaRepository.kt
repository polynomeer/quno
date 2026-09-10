package com.quno.qunobackend.infrastructure.persistence.jpa.repository

import com.quno.qunobackend.infrastructure.persistence.jpa.entity.OrganizationMembershipId
import com.quno.qunobackend.infrastructure.persistence.jpa.entity.OrganizationMembershipJpaEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface OrganizationMembershipJpaRepository : JpaRepository<OrganizationMembershipJpaEntity, OrganizationMembershipId> {
    fun countByOrganizationId(organizationId: Long): Long
    fun findAllByUserId(userId: Long): List<OrganizationMembershipJpaEntity>

    @Query(
        value = """
            SELECT organization_id AS organizationId, COUNT(*) AS memberCount FROM organization_memberships
            WHERE organization_id IN (:organizationIds)
            GROUP BY organization_id
        """,
        nativeQuery = true,
    )
    fun countMembersByOrganizations(@Param("organizationIds") organizationIds: List<Long>): List<OrganizationMemberCountRow>
}

interface OrganizationMemberCountRow {
    val organizationId: Long
    val memberCount: Long
}
