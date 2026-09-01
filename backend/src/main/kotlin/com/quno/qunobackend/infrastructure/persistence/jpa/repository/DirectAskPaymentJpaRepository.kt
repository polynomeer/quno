package com.quno.qunobackend.infrastructure.persistence.jpa.repository

import com.quno.qunobackend.infrastructure.persistence.jpa.entity.DirectAskPaymentJpaEntity
import org.springframework.data.jpa.repository.JpaRepository

interface DirectAskPaymentJpaRepository : JpaRepository<DirectAskPaymentJpaEntity, Long> {
    fun findByOrderId(orderId: String): DirectAskPaymentJpaEntity?
    fun findByDirectAskRequestId(directAskRequestId: Long): DirectAskPaymentJpaEntity?
}
