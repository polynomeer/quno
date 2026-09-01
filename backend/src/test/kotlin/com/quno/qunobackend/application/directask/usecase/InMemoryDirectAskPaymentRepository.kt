package com.quno.qunobackend.application.directask.usecase

import com.quno.qunobackend.domain.directask.DirectAskPayment
import com.quno.qunobackend.domain.directask.DirectAskPaymentRepository

class InMemoryDirectAskPaymentRepository : DirectAskPaymentRepository {
    private val paymentsById = mutableMapOf<Long, DirectAskPayment>()
    private var nextId = 1L

    override fun save(payment: DirectAskPayment): DirectAskPayment {
        val saved = if (payment.id == null) {
            DirectAskPayment.reconstitute(
                id = nextId++,
                directAskRequestId = payment.directAskRequestId,
                orderId = payment.orderId,
                amount = payment.amount,
                status = payment.status,
                tossPaymentKey = payment.tossPaymentKey,
                createdAt = payment.createdAt,
                confirmedAt = payment.confirmedAt,
                cancelledAt = payment.cancelledAt,
            )
        } else {
            payment
        }
        paymentsById[requireNotNull(saved.id)] = saved
        return saved
    }

    override fun findByOrderId(orderId: String): DirectAskPayment? = paymentsById.values.find { it.orderId == orderId }

    override fun findByDirectAskRequestId(directAskRequestId: Long): DirectAskPayment? =
        paymentsById.values.find { it.directAskRequestId == directAskRequestId }
}
