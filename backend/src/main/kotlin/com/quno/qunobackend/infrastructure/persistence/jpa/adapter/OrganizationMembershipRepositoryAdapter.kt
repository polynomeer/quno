package com.quno.qunobackend.infrastructure.persistence.jpa.adapter

import com.quno.qunobackend.domain.organization.OrganizationMembershipRepository
import com.quno.qunobackend.infrastructure.persistence.jpa.entity.OrganizationMembershipId
import com.quno.qunobackend.infrastructure.persistence.jpa.entity.OrganizationMembershipJpaEntity
import com.quno.qunobackend.infrastructure.persistence.jpa.repository.OrganizationMembershipJpaRepository
import org.springframework.stereotype.Component
import java.time.Instant

@Component
class OrganizationMembershipRepositoryAdapter(
    private val jpaRepository: OrganizationMembershipJpaRepository,
) : OrganizationMembershipRepository {

    override fun join(organizationId: Long, userId: Long) {
        val id = OrganizationMembershipId(organizationId, userId)
        if (!jpaRepository.existsById(id)) {
            jpaRepository.save(OrganizationMembershipJpaEntity(organizationId, userId, Instant.now()))
        }
    }

    override fun leave(organizationId: Long, userId: Long) {
        jpaRepository.deleteById(OrganizationMembershipId(organizationId, userId))
    }

    override fun isMember(organizationId: Long, userId: Long): Boolean =
        jpaRepository.existsById(OrganizationMembershipId(organizationId, userId))

    override fun countMembers(organizationId: Long): Long = jpaRepository.countByOrganizationId(organizationId)

    override fun findOrganizationIdsByUserId(userId: Long): List<Long> =
        jpaRepository.findAllByUserId(userId).map { it.organizationId }
}
