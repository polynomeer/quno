package com.quno.qunobackend.application.organization.usecase

import com.quno.qunobackend.domain.organization.DuplicateOrganizationNameException
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class CreateOrganizationUseCaseTest {
    private val organizationRepository = InMemoryOrganizationRepository()
    private val organizationMembershipRepository = InMemoryOrganizationMembershipRepository()
    private val useCase = CreateOrganizationUseCase(organizationRepository, organizationMembershipRepository)

    @Test
    fun `creates an organization and makes the creator its first member`() {
        val result = useCase.execute("Backend Interview Study", "Weekly problems", createdBy = 1L)

        assertEquals("Backend Interview Study", result.name)
        assertEquals(1L, result.createdBy)
        assertEquals(1L, result.memberCount)
        assertTrue(organizationMembershipRepository.isMember(result.id, 1L))
    }

    @Test
    fun `rejects a name that collides on slug with an existing organization`() {
        useCase.execute("Kotlin Study", null, createdBy = 1L)

        assertFailsWith<DuplicateOrganizationNameException> {
            useCase.execute("KOTLIN STUDY", null, createdBy = 2L)
        }
    }

    @Test
    fun `handles a Korean-only name without an empty slug collision`() {
        val first = useCase.execute("대구 백엔드 개발자 모임", null, createdBy = 1L)
        val second = useCase.execute("서울 백엔드 개발자 모임", null, createdBy = 2L)

        assertEquals("대구 백엔드 개발자 모임", first.name)
        assertEquals("서울 백엔드 개발자 모임", second.name)
    }
}
