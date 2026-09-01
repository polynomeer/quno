package com.quno.qunobackend.infrastructure.persistence.jpa.adapter

import com.quno.qunobackend.domain.organization.EmailDomainVerification
import com.quno.qunobackend.domain.organization.EmailDomainVerificationRepository
import com.quno.qunobackend.infrastructure.persistence.jpa.entity.EmailDomainVerificationJpaEntity
import com.quno.qunobackend.infrastructure.persistence.jpa.repository.EmailDomainVerificationJpaRepository
import org.springframework.stereotype.Component

@Component
class EmailDomainVerificationRepositoryAdapter(
    private val jpaRepository: EmailDomainVerificationJpaRepository,
) : EmailDomainVerificationRepository {

    override fun save(verification: EmailDomainVerification): EmailDomainVerification {
        val entity = EmailDomainVerificationJpaEntity(
            id = verification.id,
            userId = verification.userId,
            email = verification.email,
            domain = verification.domain,
            code = verification.code,
            status = verification.status,
            createdAt = verification.createdAt,
            expiresAt = verification.expiresAt,
            verifiedAt = verification.verifiedAt,
        )
        return jpaRepository.save(entity).toDomain()
    }

    override fun findLatestByUserId(userId: Long): EmailDomainVerification? =
        jpaRepository.findFirstByUserIdOrderByCreatedAtDesc(userId)?.toDomain()

    private fun EmailDomainVerificationJpaEntity.toDomain(): EmailDomainVerification = EmailDomainVerification.reconstitute(
        id = requireNotNull(id),
        userId = userId,
        email = email,
        domain = domain,
        code = code,
        status = status,
        createdAt = createdAt,
        expiresAt = expiresAt,
        verifiedAt = verifiedAt,
    )
}
