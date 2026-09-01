package com.quno.qunobackend.domain.directask

import java.time.Instant

enum class DirectAskPaymentStatus { PENDING, PAID, CANCELLED }

/**
 * One Toss Payments charge backing a [DirectAskRequest] (Phase 25, ADR-0037). `orderId` is the
 * value Quno hands to Toss's widget; `tossPaymentKey` is only known once Toss confirms the
 * charge actually happened. Kept as its own aggregate rather than fields on [DirectAskRequest]
 * because its lifecycle (PENDING → PAID → possibly CANCELLED) is driven by a different actor
 * (the payment gateway callback) than the request's own state machine.
 */
class DirectAskPayment private constructor(
    val id: Long?,
    val directAskRequestId: Long,
    val orderId: String,
    val amount: Long,
    val status: DirectAskPaymentStatus,
    val tossPaymentKey: String?,
    val createdAt: Instant,
    val confirmedAt: Instant?,
    val cancelledAt: Instant?,
) {
    fun confirm(tossPaymentKey: String): DirectAskPayment {
        check(status == DirectAskPaymentStatus.PENDING) { "payment is not pending" }
        return DirectAskPayment(id, directAskRequestId, orderId, amount, DirectAskPaymentStatus.PAID, tossPaymentKey, createdAt, Instant.now(), cancelledAt)
    }

    fun cancel(): DirectAskPayment {
        check(status == DirectAskPaymentStatus.PAID) { "only a paid payment can be cancelled" }
        return DirectAskPayment(id, directAskRequestId, orderId, amount, DirectAskPaymentStatus.CANCELLED, tossPaymentKey, createdAt, confirmedAt, Instant.now())
    }

    companion object {
        fun open(directAskRequestId: Long, orderId: String, amount: Long): DirectAskPayment = DirectAskPayment(
            id = null,
            directAskRequestId = directAskRequestId,
            orderId = orderId,
            amount = amount,
            status = DirectAskPaymentStatus.PENDING,
            tossPaymentKey = null,
            createdAt = Instant.now(),
            confirmedAt = null,
            cancelledAt = null,
        )

        fun reconstitute(
            id: Long,
            directAskRequestId: Long,
            orderId: String,
            amount: Long,
            status: DirectAskPaymentStatus,
            tossPaymentKey: String?,
            createdAt: Instant,
            confirmedAt: Instant?,
            cancelledAt: Instant?,
        ): DirectAskPayment =
            DirectAskPayment(id, directAskRequestId, orderId, amount, status, tossPaymentKey, createdAt, confirmedAt, cancelledAt)
    }
}
