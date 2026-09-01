package com.quno.qunobackend.application.organization.usecase

import com.quno.qunobackend.application.organization.dto.OrganizationResult
import com.quno.qunobackend.domain.organization.EmailDomainVerificationExpiredException
import com.quno.qunobackend.domain.organization.EmailDomainVerificationNotFoundException
import com.quno.qunobackend.domain.organization.EmailDomainVerificationRepository
import com.quno.qunobackend.domain.organization.EmailDomainVerificationStatus
import com.quno.qunobackend.domain.organization.InvalidVerificationCodeException
import com.quno.qunobackend.domain.organization.Organization
import com.quno.qunobackend.domain.organization.OrganizationMembershipRepository
import com.quno.qunobackend.domain.organization.OrganizationRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class ConfirmEmailDomainVerificationUseCase(
    private val emailDomainVerificationRepository: EmailDomainVerificationRepository,
    private val organizationRepository: OrganizationRepository,
    private val organizationMembershipRepository: OrganizationMembershipRepository,
) {
    @Transactional
    fun execute(userId: Long, code: String): OrganizationResult {
        val verification = emailDomainVerificationRepository.findLatestByUserId(userId)
            ?: throw EmailDomainVerificationNotFoundException(userId)
        if (verification.status != EmailDomainVerificationStatus.PENDING) throw EmailDomainVerificationNotFoundException(userId)
        if (verification.isExpired) throw EmailDomainVerificationExpiredException(userId)
        if (verification.code != code) throw InvalidVerificationCodeException(userId)

        emailDomainVerificationRepository.save(verification.verify())

        // Reuse an existing Verified org for this domain if one exists; otherwise upgrade a
        // same-named Virtual/Community org that happens to already exist (see ADR-0035's note
        // on this edge case); otherwise create a fresh Verified org.
        val organization = organizationRepository.findByEmailDomain(verification.domain)
            ?: organizationRepository.findBySlug(verification.domain)?.let { organizationRepository.save(it.verify(verification.domain)) }
            ?: organizationRepository.save(Organization.verifiedFor(verification.domain, createdBy = userId))

        organizationMembershipRepository.join(requireNotNull(organization.id), userId)

        return organization.toResult(memberCount = organizationMembershipRepository.countMembers(requireNotNull(organization.id)))
    }
}
