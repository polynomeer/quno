package com.quno.qunobackend.application.directask.usecase

import com.quno.qunobackend.domain.directask.ConfirmedPayment
import com.quno.qunobackend.domain.directask.PaymentConfirmationFailedException
import com.quno.qunobackend.domain.directask.PaymentGateway
import java.time.Instant

class FakePaymentGateway : PaymentGateway {
    var shouldFailConfirm: Boolean = false
    val confirmedPaymentKeys = mutableListOf<String>()
    val cancelledPaymentKeys = mutableListOf<String>()

    override fun confirm(paymentKey: String, orderId: String, amount: Long): ConfirmedPayment {
        if (shouldFailConfirm) throw PaymentConfirmationFailedException("simulated Toss rejection")
        confirmedPaymentKeys += paymentKey
        return ConfirmedPayment(paymentKey = paymentKey, orderId = orderId, totalAmount = amount, approvedAt = Instant.now())
    }

    override fun cancel(paymentKey: String, reason: String) {
        cancelledPaymentKeys += paymentKey
    }
}
