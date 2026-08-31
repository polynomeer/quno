package com.quno.qunobackend.infrastructure.persistence.jpa.entity

import com.quno.qunobackend.domain.directask.DirectAskRequestStatus
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.Instant

@Entity
@Table(name = "direct_ask_requests")
class DirectAskRequestJpaEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @Column(name = "question_id", nullable = false)
    val questionId: Long,

    @Column(name = "requester_id", nullable = false)
    val requesterId: Long,

    @Column(name = "target_user_id", nullable = false)
    val targetUserId: Long,

    @Column
    val message: String?,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    val status: DirectAskRequestStatus,

    @Column(name = "created_at", nullable = false)
    val createdAt: Instant,

    @Column(name = "responded_at")
    val respondedAt: Instant?,
)
