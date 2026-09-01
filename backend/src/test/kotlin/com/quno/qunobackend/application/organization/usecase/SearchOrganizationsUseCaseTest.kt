package com.quno.qunobackend.application.organization.usecase

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class SearchOrganizationsUseCaseTest {
    private val organizationRepository = InMemoryOrganizationRepository()
    private val organizationMembershipRepository = InMemoryOrganizationMembershipRepository()
    private val createUseCase = CreateOrganizationUseCase(organizationRepository, organizationMembershipRepository)
    private val useCase = SearchOrganizationsUseCase(organizationRepository, organizationMembershipRepository)

    @Test
    fun `filters by a case-insensitive name substring`() {
        createUseCase.execute("Kotlin Study", null, createdBy = 1L)
        createUseCase.execute("Spring Boot Migration Group", null, createdBy = 1L)

        val results = useCase.execute("kotlin")

        assertEquals(listOf("Kotlin Study"), results.map { it.name })
    }

    @Test
    fun `returns everything when the query is blank`() {
        createUseCase.execute("Kotlin Study", null, createdBy = 1L)
        createUseCase.execute("Spring Boot Migration Group", null, createdBy = 1L)

        val results = useCase.execute(null)

        assertEquals(2, results.size)
    }
}
