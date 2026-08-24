package com.quno.qunobackend.domain.common

/** Port implemented by infrastructure/persistence/jpa/adapter/OutboxEventRepositoryAdapter. */
interface OutboxEventRepository {
    fun save(event: OutboxEvent): OutboxEvent

    /** Oldest-first, for a future poller (Phase 2.8) to consume in order. */
    fun findUnpublished(limit: Int): List<OutboxEvent>
}
