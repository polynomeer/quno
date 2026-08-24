package com.quno.qunobackend.infrastructure.persistence.jpa.adapter

import com.quno.qunobackend.domain.common.OutboxEvent
import com.quno.qunobackend.domain.common.OutboxEventRepository
import com.quno.qunobackend.infrastructure.persistence.jpa.entity.OutboxEventJpaEntity
import com.quno.qunobackend.infrastructure.persistence.jpa.repository.OutboxEventJpaRepository
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Component
import java.time.Instant

@Component
class OutboxEventRepositoryAdapter(
    private val jpaRepository: OutboxEventJpaRepository,
) : OutboxEventRepository {

    override fun save(event: OutboxEvent): OutboxEvent {
        val entity = OutboxEventJpaEntity(
            id = event.id,
            eventType = event.eventType,
            aggregateType = event.aggregateType,
            aggregateId = event.aggregateId,
            payload = event.payload,
            createdAt = event.createdAt,
            publishedAt = event.publishedAt,
        )
        return jpaRepository.save(entity).toDomain()
    }

    override fun findUnpublished(limit: Int): List<OutboxEvent> =
        jpaRepository.findAllByPublishedAtIsNullOrderByCreatedAtAsc(PageRequest.of(0, limit)).map { it.toDomain() }

    override fun markPublished(id: Long) {
        jpaRepository.markPublished(id, Instant.now())
    }

    private fun OutboxEventJpaEntity.toDomain(): OutboxEvent = OutboxEvent.reconstitute(
        id = requireNotNull(id),
        eventType = eventType,
        aggregateType = aggregateType,
        aggregateId = aggregateId,
        payload = payload,
        createdAt = createdAt,
        publishedAt = publishedAt,
    )
}
