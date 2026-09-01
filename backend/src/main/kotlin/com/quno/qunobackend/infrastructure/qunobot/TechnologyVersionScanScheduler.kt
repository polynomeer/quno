package com.quno.qunobackend.infrastructure.qunobot

import com.quno.qunobackend.application.qunobot.usecase.ScanTechnologyReleasesUseCase
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

/**
 * Periodically checks endoflife.date for each tracked technology (Phase 21, ADR-0033). Real
 * release cadence is measured in days/weeks, not seconds, so this runs far less often than
 * OutboxDispatchScheduler — once a day is enough to catch a new release promptly without
 * hammering a third-party API for no reason.
 */
@Component
class TechnologyVersionScanScheduler(
    private val scanTechnologyReleasesUseCase: ScanTechnologyReleasesUseCase,
) {
    @Scheduled(fixedDelay = 24 * 60 * 60 * 1000, initialDelay = 60 * 1000)
    fun scan() {
        scanTechnologyReleasesUseCase.execute()
    }
}
