package com.quno.qunobackend.application.organization.usecase

import com.quno.qunobackend.application.organization.dto.EmailDomainVerificationResult
import com.quno.qunobackend.domain.organization.EmailDomainVerification
import com.quno.qunobackend.domain.organization.EmailDomainVerificationRepository
import com.quno.qunobackend.domain.organization.PublicEmailDomainException
import com.quno.qunobackend.domain.organization.PublicEmailDomains
import com.quno.qunobackend.domain.organization.VerificationEmailSender
import org.springframework.stereotype.Service
import kotlin.random.Random

/**
 * Sends a 6-digit code to a claimed work/school email (Phase 23, ADR-0035). Requesting again
 * before confirming simply supersedes the previous code — ConfirmEmailDomainVerificationUseCase
 * only ever looks at the latest request, so an old code silently stops working rather than
 * needing to be explicitly invalidated.
 */
@Service
class RequestEmailDomainVerificationUseCase(
    private val emailDomainVerificationRepository: EmailDomainVerificationRepository,
    private val verificationEmailSender: VerificationEmailSender,
) {
    fun execute(userId: Long, email: String): EmailDomainVerificationResult {
        val domain = email.substringAfterLast('@').lowercase()
        if (PublicEmailDomains.isBlocked(domain)) throw PublicEmailDomainException(domain)

        val code = "%06d".format(Random.nextInt(0, 1_000_000))
        val saved = emailDomainVerificationRepository.save(EmailDomainVerification.request(userId, email, domain, code))
        verificationEmailSender.sendVerificationCode(email, code)

        return EmailDomainVerificationResult(email = saved.email, expiresAt = saved.expiresAt)
    }
}
