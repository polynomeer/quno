package com.quno.qunobackend.application.organization.usecase

import com.quno.qunobackend.domain.organization.OrganizationMembershipRepository
import org.springframework.stereotype.Service

@Service
class LeaveOrganizationUseCase(
    private val organizationMembershipRepository: OrganizationMembershipRepository,
) {
    fun execute(userId: Long, organizationId: Long) {
        organizationMembershipRepository.leave(organizationId, userId)
    }
}
