package com.quno.qunobackend.application.qunobot.usecase

import com.quno.qunobackend.application.common.InMemoryOutboxEventRepository
import com.quno.qunobackend.domain.common.OutboxEventTypes
import com.quno.qunobackend.domain.qunobot.AffectedQuestion
import com.quno.qunobackend.domain.qunobot.FetchedTechnologyRelease
import org.junit.jupiter.api.Test
import java.time.LocalDate
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ApplyTechnologyReleaseUseCaseTest {
    private val technologyReleaseRepository = InMemoryTechnologyReleaseRepository()
    private val versionImpactRepository = InMemoryVersionImpactRepository()
    private val outboxEventRepository = InMemoryOutboxEventRepository()
    private val useCase = ApplyTechnologyReleaseUseCase(technologyReleaseRepository, versionImpactRepository, outboxEventRepository)

    @Test
    fun `seeds a baseline on first sight and does not notify anyone`() {
        useCase.execute("kotlin", "kotlin", FetchedTechnologyRelease("2.4.10", LocalDate.of(2026, 7, 14)))

        assertEquals("2.4.10", technologyReleaseRepository.findByTagSlug("kotlin")!!.latestVersion)
        assertTrue(outboxEventRepository.events.isEmpty())
    }

    @Test
    fun `an unchanged version on a later scan does not notify anyone`() {
        useCase.execute("kotlin", "kotlin", FetchedTechnologyRelease("2.4.10", LocalDate.of(2026, 7, 14)))

        useCase.execute("kotlin", "kotlin", FetchedTechnologyRelease("2.4.10", LocalDate.of(2026, 7, 14)))

        assertTrue(outboxEventRepository.events.isEmpty())
    }

    @Test
    fun `a genuinely new version notifies every affected question's author`() {
        useCase.execute("kotlin", "kotlin", FetchedTechnologyRelease("2.4.10", LocalDate.of(2026, 7, 14)))
        versionImpactRepository.affectedQuestionsByTag = mapOf(
            "kotlin" to listOf(AffectedQuestion(questionId = 10L, questionAuthorId = 1L), AffectedQuestion(questionId = 11L, questionAuthorId = 2L)),
        )

        useCase.execute("kotlin", "kotlin", FetchedTechnologyRelease("2.5.0", LocalDate.of(2026, 8, 20)))

        val events = outboxEventRepository.events.filter { it.eventType == OutboxEventTypes.TECH_VERSION_IMPACT_DETECTED }
        assertEquals(2, events.size)
        assertEquals(setOf(10L, 11L), events.map { it.aggregateId }.toSet())
        val event10 = events.single { it.aggregateId == 10L }
        assertTrue(event10.payload.contains("\"questionAuthorId\":1"))
        assertTrue(event10.payload.contains("\"tagSlug\":\"kotlin\""))
        assertTrue(event10.payload.contains("\"latestVersion\":\"2.5.0\""))
        assertEquals("2.5.0", technologyReleaseRepository.findByTagSlug("kotlin")!!.latestVersion)
    }

    @Test
    fun `updates checkedAt but not updatedAt when the version is unchanged`() {
        useCase.execute("redis", "redis", FetchedTechnologyRelease("8.2", LocalDate.of(2026, 6, 1)))
        val afterSeed = technologyReleaseRepository.findByTagSlug("redis")!!

        useCase.execute("redis", "redis", FetchedTechnologyRelease("8.2", LocalDate.of(2026, 6, 1)))
        val afterRecheck = technologyReleaseRepository.findByTagSlug("redis")!!

        assertEquals(afterSeed.updatedAt, afterRecheck.updatedAt)
        assertTrue(afterRecheck.checkedAt >= afterSeed.checkedAt)
    }
}
