package com.quno.qunobackend.domain.organization

/** Port for the organization_memberships relation table — pure relation data, hard delete
 * allowed, same shape as WatchRepository/UserFollowRepository. */
interface OrganizationMembershipRepository {
    /** Idempotent. */
    fun join(organizationId: Long, userId: Long)

    /** Idempotent. */
    fun leave(organizationId: Long, userId: Long)
    fun isMember(organizationId: Long, userId: Long): Boolean
    fun countMembers(organizationId: Long): Long
    fun findOrganizationIdsByUserId(userId: Long): List<Long>
}
