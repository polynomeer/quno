package com.quno.qunobackend.application.metrics.usecase

import com.quno.qunobackend.domain.metrics.MetricsRepository
import com.quno.qunobackend.domain.metrics.MetricsSnapshot
import org.springframework.stereotype.Service

/**
 * Read-only reporting model — [MetricsSnapshot] has no domain invariants to protect,
 * so it is used directly through the application and API layers instead of duplicating
 * it as a separate DTO (see docs/architecture/api-design.md "지표 계측 (Phase 4.1)").
 */
@Service
class GetMetricsSnapshotUseCase(
    private val metricsRepository: MetricsRepository,
) {
    fun execute(): MetricsSnapshot = metricsRepository.snapshot()
}
