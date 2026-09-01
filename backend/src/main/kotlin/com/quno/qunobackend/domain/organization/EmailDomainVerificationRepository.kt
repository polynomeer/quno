package com.quno.qunobackend.domain.organization

/** Port implemented by infrastructure/persistence/jpa/adapter/EmailDomainVerificationRepositoryAdapter. */
interface EmailDomainVerificationRepository {
    fun save(verification: EmailDomainVerification): EmailDomainVerification

    /** Most recent request, PENDING or not — an old superseded code becomes unreachable once a
     * newer request exists, without needing to explicitly invalidate it. */
    fun findLatestByUserId(userId: Long): EmailDomainVerification?
}
