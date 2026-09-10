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
    /** 조직마다 멤버 수를 따로 조회하던 것을 배치 쿼리로 바꿨다(quality-improvement-plan.md
     * Q-2) — `GET /organizations`가 공개 엔드포인트(ADR-0042)라 결과 개수만큼 쿼리가 나가는
     * 비용이 실제 트래픽에 그대로 반영된다. */
    fun execute(query: String?, limit: Int = 20): List<OrganizationResult> {
        val organizations = organizationRepository.search(query, limit)
        val memberCounts = organizationMembershipRepository.countMembersByOrganizations(organizations.mapNotNull { it.id })
        return organizations.map { it.toResult(memberCount = memberCounts[it.id] ?: 0L) }
    }
}
