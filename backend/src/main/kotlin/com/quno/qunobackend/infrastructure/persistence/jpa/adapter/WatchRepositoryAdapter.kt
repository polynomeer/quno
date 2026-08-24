package com.quno.qunobackend.infrastructure.persistence.jpa.adapter

import com.quno.qunobackend.domain.watch.WatchRepository
import com.quno.qunobackend.infrastructure.persistence.jpa.entity.WatchId
import com.quno.qunobackend.infrastructure.persistence.jpa.entity.WatchJpaEntity
import com.quno.qunobackend.infrastructure.persistence.jpa.repository.WatchJpaRepository
import org.springframework.stereotype.Component
import java.time.Instant

@Component
class WatchRepositoryAdapter(
    private val jpaRepository: WatchJpaRepository,
) : WatchRepository {

    override fun watch(userId: Long, questionId: Long) {
        val id = WatchId(userId, questionId)
        if (!jpaRepository.existsById(id)) {
            jpaRepository.save(WatchJpaEntity(userId, questionId, Instant.now()))
        }
    }

    override fun unwatch(userId: Long, questionId: Long) {
        jpaRepository.deleteById(WatchId(userId, questionId))
    }

    override fun isWatching(userId: Long, questionId: Long): Boolean =
        jpaRepository.existsById(WatchId(userId, questionId))

    override fun findWatchedQuestionIds(userId: Long): List<Long> =
        jpaRepository.findAllByUserId(userId).map { it.questionId }

    override fun findWatcherIds(questionId: Long): List<Long> =
        jpaRepository.findAllByQuestionId(questionId).map { it.userId }
}
