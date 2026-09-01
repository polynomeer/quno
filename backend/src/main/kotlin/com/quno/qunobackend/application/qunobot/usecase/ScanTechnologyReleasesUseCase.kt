package com.quno.qunobackend.application.qunobot.usecase

import com.quno.qunobackend.domain.qunobot.TechnologyReleaseFeed
import com.quno.qunobackend.domain.qunobot.TrackedTechnologies
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

/**
 * Entry point for the scheduled scan (infrastructure/qunobot/TechnologyVersionScanScheduler,
 * Phase 21, ADR-0033). Not `@Transactional` itself — each product's external HTTP call happens
 * here, outside any DB transaction, and only the resulting DB writes are delegated to
 * [ApplyTechnologyReleaseUseCase]. One product failing to fetch (unknown to the feed, timeout,
 * outage) is logged and skipped rather than aborting the rest of the scan.
 */
@Service
class ScanTechnologyReleasesUseCase(
    private val technologyReleaseFeed: TechnologyReleaseFeed,
    private val applyTechnologyReleaseUseCase: ApplyTechnologyReleaseUseCase,
) {
    private val logger = LoggerFactory.getLogger(ScanTechnologyReleasesUseCase::class.java)

    fun execute() {
        TrackedTechnologies.MAPPING.forEach { (tagSlug, productSlug) ->
            val fetched = technologyReleaseFeed.fetchLatest(productSlug)
            if (fetched == null) {
                logger.warn("qunobot.version-scan: no release data for productSlug={} (tagSlug={})", productSlug, tagSlug)
                return@forEach
            }
            applyTechnologyReleaseUseCase.execute(tagSlug, productSlug, fetched)
        }
    }
}
