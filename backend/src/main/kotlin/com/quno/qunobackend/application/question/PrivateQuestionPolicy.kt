package com.quno.qunobackend.application.question

import com.quno.qunobackend.domain.organization.OrganizationMembershipRepository
import com.quno.qunobackend.domain.organization.OrganizationRepository
import com.quno.qunobackend.domain.question.PrivateQuestionNotAllowedException
import org.springframework.stereotype.Component

/**
 * Who may ask an organization a private question (Phase 79, ADR-0085): a member of a *verified*
 * organization. Community organizations are open to anyone, so "members only" would mean nothing.
 */
@Component
class PrivateQuestionPolicy(
    private val organizationRepository: OrganizationRepository,
    private val membershipRepository: OrganizationMembershipRepository,
) {
    fun checkCanAsk(authorId: Long, organizationId: Long) {
        val organization = organizationRepository.findById(organizationId)
        if (organization == null || !membershipRepository.isMember(organizationId, authorId)) {
            throw PrivateQuestionNotAllowedException("only members of organization $organizationId can ask it a private question")
        }
        if (!organization.verified) {
            throw PrivateQuestionNotAllowedException("organization $organizationId is not verified; private questions need a verified organization")
        }
    }
}
