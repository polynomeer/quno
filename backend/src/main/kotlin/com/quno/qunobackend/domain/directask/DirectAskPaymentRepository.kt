package com.quno.qunobackend.domain.directask

/** Port implemented by infrastructure/persistence/jpa/adapter/DirectAskPaymentRepositoryAdapter. */
interface DirectAskPaymentRepository {
    fun save(payment: DirectAskPayment): DirectAskPayment
    fun findByOrderId(orderId: String): DirectAskPayment?
    fun findByDirectAskRequestId(directAskRequestId: Long): DirectAskPayment?
}
