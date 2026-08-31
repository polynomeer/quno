package com.quno.qunobackend.application.organization.usecase

import com.quno.qunobackend.application.organization.dto.OrganizationResult
import com.quno.qunobackend.domain.organization.OrganizationMembershipRepository
import com.quno.qunobackend.domain.organization.OrganizationRepository
import org.springframework.stereotype.Service

@Service
class SearchOrganizationsUseCase(
    private val organizationRepository: OrganizationRepository,
    private val organizationMembershipRepository: OrganizationMembershipRepository,
) {
    fun execute(query: String?, limit: Int = 20): List<OrganizationResult> =
        organizationRepository.search(query, limit).map {
            it.toResult(memberCount = organizationMembershipRepository.countMembers(requireNotNull(it.id)))
        }
}
