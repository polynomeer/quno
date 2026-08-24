package com.quno.qunobackend.domain.common

/** Port implemented by infrastructure/persistence/jpa/adapter/OutboxEventRepositoryAdapter. */
interface OutboxEventRepository {
    fun save(event: OutboxEvent): OutboxEvent

    /** Oldest-first, for the dispatch poller to consume in order. */
    fun findUnpublished(limit: Int): List<OutboxEvent>

    fun markPublished(id: Long)
}
