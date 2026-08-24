package com.quno.qunobackend.application.common

import com.quno.qunobackend.domain.common.OutboxEvent
import com.quno.qunobackend.domain.common.OutboxEventRepository
import java.time.Instant

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

    override fun markPublished(id: Long) {
        val index = events.indexOfFirst { it.id == id }
        if (index == -1) return
        val event = events[index]
        events[index] = OutboxEvent.reconstitute(
            id = requireNotNull(event.id),
            eventType = event.eventType,
            aggregateType = event.aggregateType,
            aggregateId = event.aggregateId,
            payload = event.payload,
            createdAt = event.createdAt,
            publishedAt = Instant.now(),
        )
    }
}
