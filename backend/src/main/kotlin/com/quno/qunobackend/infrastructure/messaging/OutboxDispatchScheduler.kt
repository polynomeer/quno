package com.quno.qunobackend.infrastructure.messaging

import com.quno.qunobackend.application.notification.usecase.DispatchOutboxEventsUseCase
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

/**
 * Stands in for the "Outbox Publisher" in docs/architecture/system-architecture.md — an
 * in-process poller is enough for a modular monolith at MVP scale. Swap for a Redis/Kafka
 * consumer if this ever needs to run out-of-process.
 *
 * `quno.outbox.dispatch-scheduler.enabled=false` drops the bean entirely. The Gradle test task
 * sets it so integration tests dispatch only through explicit
 * [DispatchOutboxEventsUseCase.execute] calls — a background poller (one per cached Spring test
 * context) would otherwise commit notifications mid-cleanup and break the users FK
 * (technical-deep-dives.md).
 */
@Component
@ConditionalOnProperty(prefix = "quno.outbox.dispatch-scheduler", name = ["enabled"], havingValue = "true", matchIfMissing = true)
class OutboxDispatchScheduler(
    private val dispatchOutboxEventsUseCase: DispatchOutboxEventsUseCase,
) {
    @Scheduled(fixedDelay = 2000)
    fun dispatch() {
        dispatchOutboxEventsUseCase.execute()
    }
}
