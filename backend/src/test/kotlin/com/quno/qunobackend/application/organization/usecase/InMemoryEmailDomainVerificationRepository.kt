package com.quno.qunobackend.application.organization.usecase

import com.quno.qunobackend.domain.organization.EmailDomainVerification
import com.quno.qunobackend.domain.organization.EmailDomainVerificationRepository

class InMemoryEmailDomainVerificationRepository : EmailDomainVerificationRepository {
    private val byId = mutableMapOf<Long, EmailDomainVerification>()
    private var nextId = 1L

    override fun save(verification: EmailDomainVerification): EmailDomainVerification {
        val saved = if (verification.id == null) {
            EmailDomainVerification.reconstitute(
                id = nextId++,
                userId = verification.userId,
                email = verification.email,
                domain = verification.domain,
                code = verification.code,
                status = verification.status,
                createdAt = verification.createdAt,
                expiresAt = verification.expiresAt,
                verifiedAt = verification.verifiedAt,
            )
        } else {
            verification
        }
        byId[requireNotNull(saved.id)] = saved
        return saved
    }

    // Keyed by id (monotonically increasing on save), not createdAt — two requests made in the
    // same test can share an Instant.now() value at low clock resolution, which would make a
    // createdAt-based "latest" pick arbitrarily.
    override fun findLatestByUserId(userId: Long): EmailDomainVerification? =
        byId.values.filter { it.userId == userId }.maxByOrNull { requireNotNull(it.id) }
}
