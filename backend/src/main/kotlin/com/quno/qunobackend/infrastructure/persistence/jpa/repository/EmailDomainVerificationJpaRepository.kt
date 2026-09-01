package com.quno.qunobackend.infrastructure.persistence.jpa.repository

import com.quno.qunobackend.infrastructure.persistence.jpa.entity.EmailDomainVerificationJpaEntity
import org.springframework.data.jpa.repository.JpaRepository

interface EmailDomainVerificationJpaRepository : JpaRepository<EmailDomainVerificationJpaEntity, Long> {
    fun findFirstByUserIdOrderByCreatedAtDesc(userId: Long): EmailDomainVerificationJpaEntity?
}
