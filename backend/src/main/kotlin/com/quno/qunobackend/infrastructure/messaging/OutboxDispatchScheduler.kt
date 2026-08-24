package com.quno.qunobackend.infrastructure.messaging

import com.quno.qunobackend.application.notification.usecase.DispatchOutboxEventsUseCase
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

/**
 * Stands in for the "Outbox Publisher" in docs/architecture/system-architecture.md — an
 * in-process poller is enough for a modular monolith at MVP scale. Swap for a Redis/Kafka
 * consumer if this ever needs to run out-of-process.
 */
@Component
class OutboxDispatchScheduler(
    private val dispatchOutboxEventsUseCase: DispatchOutboxEventsUseCase,
) {
    @Scheduled(fixedDelay = 2000)
    fun dispatch() {
        dispatchOutboxEventsUseCase.execute()
    }
}
