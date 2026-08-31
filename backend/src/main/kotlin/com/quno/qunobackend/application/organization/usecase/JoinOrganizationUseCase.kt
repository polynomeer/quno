package com.quno.qunobackend.application.organization.usecase

import com.quno.qunobackend.domain.organization.OrganizationMembershipRepository
import com.quno.qunobackend.domain.organization.OrganizationNotFoundException
import com.quno.qunobackend.domain.organization.OrganizationRepository
import org.springframework.stereotype.Service

@Service
class JoinOrganizationUseCase(
    private val organizationRepository: OrganizationRepository,
    private val organizationMembershipRepository: OrganizationMembershipRepository,
) {
    fun execute(userId: Long, organizationId: Long) {
        organizationRepository.findById(organizationId) ?: throw OrganizationNotFoundException(organizationId)
        organizationMembershipRepository.join(organizationId, userId)
    }
}
