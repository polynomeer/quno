package com.quno.qunobackend.infrastructure.persistence.jpa.entity

import com.quno.qunobackend.domain.directask.DirectAskPaymentStatus
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
@Table(name = "direct_ask_payments")
class DirectAskPaymentJpaEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @Column(name = "direct_ask_request_id", nullable = false)
    val directAskRequestId: Long,

    @Column(name = "order_id", nullable = false)
    val orderId: String,

    @Column(nullable = false)
    val amount: Long,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    val status: DirectAskPaymentStatus,

    @Column(name = "toss_payment_key")
    val tossPaymentKey: String?,

    @Column(name = "created_at", nullable = false)
    val createdAt: Instant,

    @Column(name = "confirmed_at")
    val confirmedAt: Instant?,

    @Column(name = "cancelled_at")
    val cancelledAt: Instant?,
)
