package com.quno.qunobackend.application.organization.usecase

import com.quno.qunobackend.domain.organization.OrganizationNotFoundException
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class JoinAndLeaveOrganizationUseCaseTest {
    private val organizationRepository = InMemoryOrganizationRepository()
    private val organizationMembershipRepository = InMemoryOrganizationMembershipRepository()
    private val createUseCase = CreateOrganizationUseCase(organizationRepository, organizationMembershipRepository)
    private val joinUseCase = JoinOrganizationUseCase(organizationRepository, organizationMembershipRepository)
    private val leaveUseCase = LeaveOrganizationUseCase(organizationMembershipRepository)
    private val getUseCase = GetOrganizationUseCase(organizationRepository, organizationMembershipRepository)

    @Test
    fun `a second user can join, growing the member count`() {
        val organizationId = createUseCase.execute("Study", null, createdBy = 1L).id

        joinUseCase.execute(2L, organizationId)

        assertEquals(2L, getUseCase.execute(organizationId).memberCount)
    }

    @Test
    fun `joining twice is idempotent`() {
        val organizationId = createUseCase.execute("Study", null, createdBy = 1L).id

        joinUseCase.execute(2L, organizationId)
        joinUseCase.execute(2L, organizationId)

        assertEquals(2L, getUseCase.execute(organizationId).memberCount)
    }

    @Test
    fun `leaving removes membership`() {
        val organizationId = createUseCase.execute("Study", null, createdBy = 1L).id
        joinUseCase.execute(2L, organizationId)

        leaveUseCase.execute(2L, organizationId)

        assertFalse(organizationMembershipRepository.isMember(organizationId, 2L))
        assertTrue(organizationMembershipRepository.isMember(organizationId, 1L))
    }

    @Test
    fun `joining a non-existent organization fails`() {
        assertFailsWith<OrganizationNotFoundException> { joinUseCase.execute(1L, 999L) }
    }
}
