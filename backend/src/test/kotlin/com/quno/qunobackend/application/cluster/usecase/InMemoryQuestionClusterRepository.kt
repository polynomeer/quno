package com.quno.qunobackend.application.cluster.usecase

import com.quno.qunobackend.domain.cluster.QuestionCluster
import com.quno.qunobackend.domain.cluster.QuestionClusterRepository

class InMemoryQuestionClusterRepository : QuestionClusterRepository {
    private val byId = mutableMapOf<Long, QuestionCluster>()
    private var nextId = 1L

    override fun save(cluster: QuestionCluster): QuestionCluster {
        val saved = if (cluster.id == null) {
            QuestionCluster.reconstitute(
                id = nextId++,
                representativeAnswerId = cluster.representativeAnswerId,
                createdAt = cluster.createdAt,
                updatedAt = cluster.updatedAt,
            )
        } else {
            cluster
        }
        byId[requireNotNull(saved.id)] = saved
        return saved
    }

    override fun findById(id: Long): QuestionCluster? = byId[id]
}
