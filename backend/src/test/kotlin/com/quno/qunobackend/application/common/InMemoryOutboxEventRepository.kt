package com.quno.qunobackend.application.common

import com.quno.qunobackend.domain.common.OutboxEvent
import com.quno.qunobackend.domain.common.OutboxEventRepository

class InMemoryOutboxEventRepository : OutboxEventRepository {
    val events = mutableListOf<OutboxEvent>()
    private var nextId = 1L

    override fun save(event: OutboxEvent): OutboxEvent {
        val saved = if (event.id == null) {
            OutboxEvent.reconstitute(
                id = nextId++,
                eventType = event.eventType,
                aggregateType = event.aggregateType,
                aggregateId = event.aggregateId,
                payload = event.payload,
                createdAt = event.createdAt,
                publishedAt = event.publishedAt,
            )
        } else {
            event
        }
        events += saved
        return saved
    }

    override fun findUnpublished(limit: Int): List<OutboxEvent> =
        events.filter { it.publishedAt == null }.take(limit)
}
