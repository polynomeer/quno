package com.quno.qunobackend.application.qunobot.usecase

import com.quno.qunobackend.application.common.InMemoryOutboxEventRepository
import com.quno.qunobackend.domain.qunobot.FetchedTechnologyRelease
import com.quno.qunobackend.domain.qunobot.TrackedTechnologies
import org.junit.jupiter.api.Test
import java.time.LocalDate
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ScanTechnologyReleasesUseCaseTest {
    private val technologyReleaseRepository = InMemoryTechnologyReleaseRepository()
    private val versionImpactRepository = InMemoryVersionImpactRepository()
    private val outboxEventRepository = InMemoryOutboxEventRepository()
    private val applyTechnologyReleaseUseCase =
        ApplyTechnologyReleaseUseCase(technologyReleaseRepository, versionImpactRepository, outboxEventRepository)
    private val feed = FakeTechnologyReleaseFeed()
    private val useCase = ScanTechnologyReleasesUseCase(feed, applyTechnologyReleaseUseCase)

    @Test
    fun `queries the feed for every tracked product, once each`() {
        useCase.execute()

        assertEquals(TrackedTechnologies.MAPPING.values.sorted(), feed.fetchedProductSlugs.sorted())
    }

    @Test
    fun `seeds a baseline for every product the feed knows about`() {
        feed.releasesByProductSlug = mapOf(
            "kotlin" to FetchedTechnologyRelease("2.4.10", LocalDate.of(2026, 7, 14)),
            "redis" to FetchedTechnologyRelease("8.2", LocalDate.of(2026, 6, 1)),
        )

        useCase.execute()

        assertEquals("2.4.10", technologyReleaseRepository.findByTagSlug("kotlin")!!.latestVersion)
        assertEquals("8.2", technologyReleaseRepository.findByTagSlug("redis")!!.latestVersion)
        assertTrue(technologyReleaseRepository.findByTagSlug("spring-boot") == null)
    }

    @Test
    fun `a product the feed has no data for is skipped without breaking the rest of the scan`() {
        feed.releasesByProductSlug = mapOf("kotlin" to FetchedTechnologyRelease("2.4.10", LocalDate.of(2026, 7, 14)))

        useCase.execute()

        assertEquals("2.4.10", technologyReleaseRepository.findByTagSlug("kotlin")!!.latestVersion)
        assertTrue(technologyReleaseRepository.findByTagSlug("kafka") == null)
    }
}
