package com.quno.qunobackend.infrastructure.persistence.jpa.adapter

import com.quno.qunobackend.domain.cluster.QuestionCluster
import com.quno.qunobackend.domain.cluster.QuestionClusterRepository
import com.quno.qunobackend.infrastructure.persistence.jpa.entity.QuestionClusterJpaEntity
import com.quno.qunobackend.infrastructure.persistence.jpa.repository.QuestionClusterJpaRepository
import org.springframework.stereotype.Component

@Component
class QuestionClusterRepositoryAdapter(
    private val jpaRepository: QuestionClusterJpaRepository,
) : QuestionClusterRepository {

    override fun save(cluster: QuestionCluster): QuestionCluster {
        val entity = QuestionClusterJpaEntity(
            id = cluster.id,
            representativeAnswerId = cluster.representativeAnswerId,
            createdAt = cluster.createdAt,
        )
        return jpaRepository.save(entity).toDomain()
    }

    override fun findById(id: Long): QuestionCluster? = jpaRepository.findById(id).orElse(null)?.toDomain()

    private fun QuestionClusterJpaEntity.toDomain(): QuestionCluster = QuestionCluster.reconstitute(
        id = requireNotNull(id),
        representativeAnswerId = representativeAnswerId,
        createdAt = createdAt,
    )
}
