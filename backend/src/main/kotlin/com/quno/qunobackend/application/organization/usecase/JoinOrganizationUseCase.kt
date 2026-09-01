package com.quno.qunobackend.application.organization.usecase

import com.quno.qunobackend.domain.organization.OrganizationMembershipRepository
import com.quno.qunobackend.domain.organization.OrganizationNotFoundException
import com.quno.qunobackend.domain.organization.OrganizationRepository
import com.quno.qunobackend.domain.organization.VerifiedOrganizationJoinRequiresEmailException
import org.springframework.stereotype.Service

@Service
class JoinOrganizationUseCase(
    private val organizationRepository: OrganizationRepository,
    private val organizationMembershipRepository: OrganizationMembershipRepository,
) {
    fun execute(userId: Long, organizationId: Long) {
        val organization = organizationRepository.findById(organizationId) ?: throw OrganizationNotFoundException(organizationId)
        // A Verified organization's membership is only ever granted through email verification
        // (Phase 23, ADR-0035) — this endpoint would otherwise let anyone fake belonging to it.
        if (organization.verified) throw VerifiedOrganizationJoinRequiresEmailException(organizationId)
        organizationMembershipRepository.join(organizationId, userId)
    }
}
