package com.quno.qunobackend.infrastructure.persistence.jpa.entity

import com.quno.qunobackend.domain.vote.VoteTargetType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Id
import jakarta.persistence.IdClass
import jakarta.persistence.Table
import java.io.Serializable
import java.time.Instant

data class VoteId(
    val voterId: Long = 0,
    val targetType: VoteTargetType = VoteTargetType.QUESTION,
    val targetId: Long = 0,
) : Serializable

@Entity
@Table(name = "votes")
@IdClass(VoteId::class)
class VoteJpaEntity(
    @Id
    @Column(name = "voter_id")
    val voterId: Long,

    @Id
    @Column(name = "target_type")
    @Enumerated(EnumType.STRING)
    val targetType: VoteTargetType,

    @Id
    @Column(name = "target_id")
    val targetId: Long,

    @Column(name = "value", nullable = false)
    val value: Int,

    @Column(name = "created_at", nullable = false)
    val createdAt: Instant,

    @Column(name = "updated_at", nullable = false)
    val updatedAt: Instant,
)
