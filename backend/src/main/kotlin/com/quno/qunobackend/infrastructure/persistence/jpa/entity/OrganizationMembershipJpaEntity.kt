package com.quno.qunobackend.infrastructure.persistence.jpa.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.IdClass
import jakarta.persistence.Table
import java.io.Serializable
import java.time.Instant

data class OrganizationMembershipId(
    val organizationId: Long = 0,
    val userId: Long = 0,
) : Serializable

@Entity
@Table(name = "organization_memberships")
@IdClass(OrganizationMembershipId::class)
class OrganizationMembershipJpaEntity(
    @Id
    @Column(name = "organization_id")
    val organizationId: Long,

    @Id
    @Column(name = "user_id")
    val userId: Long,

    @Column(name = "joined_at", nullable = false)
    val joinedAt: Instant,
)
