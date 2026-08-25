package com.quno.qunobackend.infrastructure.persistence.jpa.adapter

import com.quno.qunobackend.domain.metrics.MetricsRepository
import com.quno.qunobackend.domain.metrics.MetricsSnapshot
import com.quno.qunobackend.infrastructure.persistence.jpa.repository.MetricsJpaRepository
import org.springframework.stereotype.Component

@Component
class MetricsRepositoryAdapter(
    private val jpaRepository: MetricsJpaRepository,
) : MetricsRepository {

    override fun snapshot(): MetricsSnapshot {
        val row = jpaRepository.snapshot()
        return MetricsSnapshot(
            totalQuestions = row.getTotalQuestions(),
            revisedQuestions = row.getRevisedQuestions(),
            answeredQuestions = row.getAnsweredQuestions(),
            resolvedQuestions = row.getResolvedQuestions(),
            watchedQuestions = row.getWatchedQuestions(),
            livingQuestions = row.getLivingQuestions(),
        )
    }
}
