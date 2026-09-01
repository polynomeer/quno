package com.quno.qunobackend.application.organization.usecase

import com.quno.qunobackend.domain.organization.OrganizationMembershipRepository

class InMemoryOrganizationMembershipRepository : OrganizationMembershipRepository {
    private val memberships = mutableSetOf<Pair<Long, Long>>()

    override fun join(organizationId: Long, userId: Long) {
        memberships += organizationId to userId
    }

    override fun leave(organizationId: Long, userId: Long) {
        memberships -= organizationId to userId
    }

    override fun isMember(organizationId: Long, userId: Long): Boolean = (organizationId to userId) in memberships

    override fun countMembers(organizationId: Long): Long = memberships.count { it.first == organizationId }.toLong()

    override fun findOrganizationIdsByUserId(userId: Long): List<Long> =
        memberships.filter { it.second == userId }.map { it.first }
}
