package com.quno.qunobackend.application.qunobot.usecase

import com.quno.qunobackend.application.answer.usecase.InMemoryAnswerApplicabilityRepository
import com.quno.qunobackend.application.common.InMemoryOutboxEventRepository
import com.quno.qunobackend.domain.common.OutboxEventTypes
import com.quno.qunobackend.domain.qunobot.AffectedQuestion
import com.quno.qunobackend.domain.qunobot.FetchedTechnologyRelease
import com.quno.qunobackend.domain.qunobot.VersionImpact
import com.quno.qunobackend.domain.stack.ApplicabilityRange
import com.quno.qunobackend.domain.stack.Ecosystem
import org.junit.jupiter.api.Test
import java.time.LocalDate
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ApplyTechnologyReleaseUseCaseTest {
    private val technologyReleaseRepository = InMemoryTechnologyReleaseRepository()
    private val versionImpactRepository = InMemoryVersionImpactRepository()
    private val outboxEventRepository = InMemoryOutboxEventRepository()
    private val applicabilityRepository = InMemoryAnswerApplicabilityRepository()
    private val useCase = ApplyTechnologyReleaseUseCase(technologyReleaseRepository, versionImpactRepository, outboxEventRepository, applicabilityRepository)

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

    @Test
    fun `skips questions whose answer range still covers the new version and labels the rest`() {
        useCase.execute("spring-boot", "spring-boot", FetchedTechnologyRelease("3.4.0", LocalDate.of(2026, 7, 1)))
        versionImpactRepository.affectedQuestionsByTag = mapOf(
            "spring-boot" to listOf(
                AffectedQuestion(questionId = 20L, questionAuthorId = 1L),
                AffectedQuestion(questionId = 21L, questionAuthorId = 2L),
                AffectedQuestion(questionId = 22L, questionAuthorId = 3L),
            ),
        )
        applicabilityRepository.replaceForAnswer(200L, 20L, 1, listOf(ApplicabilityRange(Ecosystem.GRADLE_PLUGIN, "org.springframework.boot", "3.2.0", "4.0.0")))
        applicabilityRepository.replaceForAnswer(210L, 21L, 1, listOf(ApplicabilityRange(Ecosystem.MAVEN, "org.springframework.boot:spring-boot-starter-web", null, "3.5.0")))

        useCase.execute("spring-boot", "spring-boot", FetchedTechnologyRelease("3.5.0", LocalDate.of(2026, 8, 1)))

        val events = outboxEventRepository.events.filter { it.eventType == OutboxEventTypes.TECH_VERSION_IMPACT_DETECTED }
        assertEquals(setOf(21L, 22L), events.map { it.aggregateId }.toSet(), "20's answer holds below 4.0, so 3.5.0 is still covered")
        assertTrue(events.single { it.aggregateId == 21L }.payload.contains("\"reason\":\"OUT_OF_RANGE\""))
        assertTrue(events.single { it.aggregateId == 22L }.payload.contains("\"reason\":\"NO_RANGE\""))
    }

    @Test
    fun `ranges are ignored for a tag without package patterns`() {
        useCase.execute("redis", "redis", FetchedTechnologyRelease("8.2", LocalDate.of(2026, 6, 1)))
        versionImpactRepository.affectedQuestionsByTag = mapOf("redis" to listOf(AffectedQuestion(questionId = 30L, questionAuthorId = 1L)))
        applicabilityRepository.replaceForAnswer(300L, 30L, 1, listOf(ApplicabilityRange(Ecosystem.NPM, "redis", "4.0.0", null)))

        useCase.execute("redis", "redis", FetchedTechnologyRelease("8.4", LocalDate.of(2026, 9, 1)))

        val event = outboxEventRepository.events.single { it.eventType == OutboxEventTypes.TECH_VERSION_IMPACT_DETECTED }
        assertTrue(event.payload.contains("\"reason\":\"NO_RANGE\""))
    }

    @Test
    fun `the version impact list applies the same rule`() {
        versionImpactRepository.versionImpacts = listOf(
            VersionImpact(40L, "covered", "kotlin", "kotlin", "2.3.0", LocalDate.of(2026, 9, 1)),
            VersionImpact(41L, "not covered", "kotlin", "kotlin", "2.3.0", LocalDate.of(2026, 9, 1)),
        )
        applicabilityRepository.replaceForAnswer(400L, 40L, 1, listOf(ApplicabilityRange(Ecosystem.GRADLE_PLUGIN, "org.jetbrains.kotlin.jvm", "2.0.0", "3.0.0")))

        val impacts = GetVersionImpactsUseCase(versionImpactRepository, applicabilityRepository).execute(limit = 10)

        assertEquals(listOf(41L), impacts.map { it.questionId })
    }
}
