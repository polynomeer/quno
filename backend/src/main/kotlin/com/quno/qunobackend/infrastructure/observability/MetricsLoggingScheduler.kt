package com.quno.qunobackend.infrastructure.observability

import com.quno.qunobackend.application.metrics.usecase.GetMetricsSnapshotUseCase
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

/**
 * Periodically logs the MVP success-indicator snapshot (docs/product/mvp-scope.md
 * "성공 지표") so it can be scraped by log-based observability tooling without a
 * dedicated metrics backend. See also GET /api/v1/metrics for on-demand checks.
 */
@Component
class MetricsLoggingScheduler(
    private val getMetricsSnapshotUseCase: GetMetricsSnapshotUseCase,
) {
    private val logger = LoggerFactory.getLogger(MetricsLoggingScheduler::class.java)

    @Scheduled(fixedDelay = 30 * 60 * 1000)
    fun logSnapshot() {
        val snapshot = getMetricsSnapshotUseCase.execute()
        logger.info(
            "quno.metrics totalQuestions={} revisionRate={} answerRate={} acceptRate={} wardCoverageRate={} livingQuestionRate={}",
            snapshot.totalQuestions,
            "%.3f".format(snapshot.revisionRate),
            "%.3f".format(snapshot.answerRate),
            "%.3f".format(snapshot.acceptRate),
            "%.3f".format(snapshot.wardCoverageRate),
            "%.3f".format(snapshot.livingQuestionRate),
        )
    }
}
