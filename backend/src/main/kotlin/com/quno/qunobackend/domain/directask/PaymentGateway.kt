package com.quno.qunobackend.domain.directask

import java.time.Instant

data class ConfirmedPayment(val paymentKey: String, val orderId: String, val totalAmount: Long, val approvedAt: Instant)

/**
 * Outbound port to Toss Payments' Core API (Phase 25, ADR-0037) — implemented by
 * infrastructure/external/TossPaymentGateway. Card data itself never reaches this codebase: the
 * client-side widget (not built in this backend-only Phase, see ADR-0020) submits directly to
 * Toss, and Quno only ever handles the `paymentKey`/`orderId` pair Toss hands back.
 */
interface PaymentGateway {
    /** @throws PaymentConfirmationFailedException if Toss rejects the confirm call. */
    fun confirm(paymentKey: String, orderId: String, amount: Long): ConfirmedPayment

    /** @throws PaymentConfirmationFailedException if Toss rejects the cancel call. */
    fun cancel(paymentKey: String, reason: String)
}
