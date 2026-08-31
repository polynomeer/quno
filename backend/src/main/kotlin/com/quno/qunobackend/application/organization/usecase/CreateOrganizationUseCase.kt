package com.quno.qunobackend.application.organization.usecase

import com.quno.qunobackend.application.organization.dto.OrganizationResult
import com.quno.qunobackend.domain.organization.DuplicateOrganizationNameException
import com.quno.qunobackend.domain.organization.Organization
import com.quno.qunobackend.domain.organization.OrganizationMembershipRepository
import com.quno.qunobackend.domain.organization.OrganizationRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/** The creator automatically becomes the first member — a study group with no one in it isn't
 * useful, and the original brainstorm always frames organizations as something you're "in". */
@Service
class CreateOrganizationUseCase(
    private val organizationRepository: OrganizationRepository,
    private val organizationMembershipRepository: OrganizationMembershipRepository,
) {
    @Transactional
    fun execute(name: String, description: String?, createdBy: Long): OrganizationResult {
        val candidate = Organization.create(name, description, createdBy)
        organizationRepository.findBySlug(candidate.slug)?.let { throw DuplicateOrganizationNameException(name) }

        val saved = organizationRepository.save(candidate)
        organizationMembershipRepository.join(requireNotNull(saved.id), createdBy)

        return saved.toResult(memberCount = 1)
    }
}

internal fun Organization.toResult(memberCount: Long) = OrganizationResult(
    id = requireNotNull(id),
    name = name,
    description = description,
    createdBy = createdBy,
    memberCount = memberCount,
    createdAt = createdAt,
)
