package com.quno.qunobackend.application.organization.usecase

import com.quno.qunobackend.application.organization.dto.OrganizationResult
import com.quno.qunobackend.domain.organization.OrganizationMembershipRepository
import com.quno.qunobackend.domain.organization.OrganizationNotFoundException
import com.quno.qunobackend.domain.organization.OrganizationRepository
import org.springframework.stereotype.Service

@Service
class GetOrganizationUseCase(
    private val organizationRepository: OrganizationRepository,
    private val organizationMembershipRepository: OrganizationMembershipRepository,
) {
    fun execute(id: Long): OrganizationResult {
        val organization = organizationRepository.findById(id) ?: throw OrganizationNotFoundException(id)
        return organization.toResult(memberCount = organizationMembershipRepository.countMembers(id))
    }
}
