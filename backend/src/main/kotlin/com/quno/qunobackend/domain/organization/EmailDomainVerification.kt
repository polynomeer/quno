package com.quno.qunobackend.domain.organization

import java.time.Duration
import java.time.Instant

enum class EmailDomainVerificationStatus { PENDING, VERIFIED }

/**
 * A one-time code sent to a user's claimed work/school email, proving they control an address at
 * that domain (Phase 23, ADR-0035) — the only route to becoming a member of a Verified
 * organization. Expiry is checked live against [expiresAt] rather than a background job flipping
 * status, matching this codebase's preference for computed state over scheduled housekeeping.
 */
class EmailDomainVerification private constructor(
    val id: Long?,
    val userId: Long,
    val email: String,
    val domain: String,
    val code: String,
    val status: EmailDomainVerificationStatus,
    val createdAt: Instant,
    val expiresAt: Instant,
    val verifiedAt: Instant?,
) {
    val isExpired: Boolean get() = Instant.now().isAfter(expiresAt)

    /** Precondition (status/expiry) is validated by ConfirmEmailDomainVerificationUseCase before
     * calling this — the checks here are a defensive invariant, same pattern as ReviewRequest.addressed(). */
    fun verify(): EmailDomainVerification {
        check(status == EmailDomainVerificationStatus.PENDING) { "already verified" }
        check(!isExpired) { "verification code expired" }
        return EmailDomainVerification(id, userId, email, domain, code, EmailDomainVerificationStatus.VERIFIED, createdAt, expiresAt, Instant.now())
    }

    companion object {
        private val TTL: Duration = Duration.ofMinutes(15)

        fun request(userId: Long, email: String, domain: String, code: String): EmailDomainVerification {
            val now = Instant.now()
            return EmailDomainVerification(
                id = null,
                userId = userId,
                email = email,
                domain = domain,
                code = code,
                status = EmailDomainVerificationStatus.PENDING,
                createdAt = now,
                expiresAt = now.plus(TTL),
                verifiedAt = null,
            )
        }

        fun reconstitute(
            id: Long,
            userId: Long,
            email: String,
            domain: String,
            code: String,
            status: EmailDomainVerificationStatus,
            createdAt: Instant,
            expiresAt: Instant,
            verifiedAt: Instant?,
        ): EmailDomainVerification = EmailDomainVerification(id, userId, email, domain, code, status, createdAt, expiresAt, verifiedAt)
    }
}
