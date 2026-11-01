package com.quno.qunobackend.infrastructure.messaging

import com.quno.qunobackend.application.engine.SweepQuestionEnginesUseCase
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

/**
 * Backfills the engine layer (Phase 78, ADR-0081): every tick, a batch of questions whose latest
 * version the current engine version hasn't read. Bumping an engine version re-reads everything
 * this way. Off in the Gradle test task for the same reason as [OutboxDispatchScheduler].
 */
@Component
@ConditionalOnProperty(prefix = "quno.engine.sweep", name = ["enabled"], havingValue = "true", matchIfMissing = true)
class EngineSweepScheduler(
    private val sweepQuestionEnginesUseCase: SweepQuestionEnginesUseCase,
    @Value("\${quno.engine.sweep.batch-size:50}") private val batchSize: Int,
) {
    @Scheduled(fixedDelayString = "\${quno.engine.sweep.interval-ms:60000}", initialDelay = 30_000)
    fun sweep() {
        sweepQuestionEnginesUseCase.execute(batchSize)
    }
}
