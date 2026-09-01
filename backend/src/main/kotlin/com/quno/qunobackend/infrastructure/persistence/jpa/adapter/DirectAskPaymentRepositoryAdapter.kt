package com.quno.qunobackend.infrastructure.persistence.jpa.adapter

import com.quno.qunobackend.domain.directask.DirectAskPayment
import com.quno.qunobackend.domain.directask.DirectAskPaymentRepository
import com.quno.qunobackend.infrastructure.persistence.jpa.entity.DirectAskPaymentJpaEntity
import com.quno.qunobackend.infrastructure.persistence.jpa.repository.DirectAskPaymentJpaRepository
import org.springframework.stereotype.Component

@Component
class DirectAskPaymentRepositoryAdapter(
    private val jpaRepository: DirectAskPaymentJpaRepository,
) : DirectAskPaymentRepository {

    override fun save(payment: DirectAskPayment): DirectAskPayment {
        val entity = DirectAskPaymentJpaEntity(
            id = payment.id,
            directAskRequestId = payment.directAskRequestId,
            orderId = payment.orderId,
            amount = payment.amount,
            status = payment.status,
            tossPaymentKey = payment.tossPaymentKey,
            createdAt = payment.createdAt,
            confirmedAt = payment.confirmedAt,
            cancelledAt = payment.cancelledAt,
        )
        return jpaRepository.save(entity).toDomain()
    }

    override fun findByOrderId(orderId: String): DirectAskPayment? = jpaRepository.findByOrderId(orderId)?.toDomain()

    override fun findByDirectAskRequestId(directAskRequestId: Long): DirectAskPayment? =
        jpaRepository.findByDirectAskRequestId(directAskRequestId)?.toDomain()

    private fun DirectAskPaymentJpaEntity.toDomain(): DirectAskPayment = DirectAskPayment.reconstitute(
        id = requireNotNull(id),
        directAskRequestId = directAskRequestId,
        orderId = orderId,
        amount = amount,
        status = status,
        tossPaymentKey = tossPaymentKey,
        createdAt = createdAt,
        confirmedAt = confirmedAt,
        cancelledAt = cancelledAt,
    )
}
