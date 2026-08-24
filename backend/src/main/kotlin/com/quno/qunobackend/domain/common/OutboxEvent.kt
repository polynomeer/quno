package com.quno.qunobackend.domain.common

import java.time.Instant

/**
 * Transactional Outbox row — written in the same DB transaction as the domain change it
 * describes, so a lost commit can never race a lost event. A consumer worker that fans
 * these out to Ward(Watch) subscribers is added with Notification (PLAN.md Phase 2.8);
 * this is just the producer-side skeleton. Payload is a pre-serialized JSON string —
 * kept a plain String so this class stays framework-free.
 */
class OutboxEvent private constructor(
    val id: Long?,
    val eventType: String,
    val aggregateType: String,
    val aggregateId: Long,
    val payload: String,
    val createdAt: Instant,
    val publishedAt: Instant?,
) {
    companion object {
        fun create(eventType: String, aggregateType: String, aggregateId: Long, payload: String): OutboxEvent =
            OutboxEvent(
                id = null,
                eventType = eventType,
                aggregateType = aggregateType,
                aggregateId = aggregateId,
                payload = payload,
                createdAt = Instant.now(),
                publishedAt = null,
            )

        fun reconstitute(
            id: Long,
            eventType: String,
            aggregateType: String,
            aggregateId: Long,
            payload: String,
            createdAt: Instant,
            publishedAt: Instant?,
        ): OutboxEvent = OutboxEvent(id, eventType, aggregateType, aggregateId, payload, createdAt, publishedAt)
    }
}
