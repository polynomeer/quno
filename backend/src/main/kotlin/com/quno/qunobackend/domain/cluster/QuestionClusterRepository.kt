package com.quno.qunobackend.domain.cluster

/** Port implemented by infrastructure/persistence/jpa/adapter/QuestionClusterRepositoryAdapter. */
interface QuestionClusterRepository {
    fun save(cluster: QuestionCluster): QuestionCluster
    fun findById(id: Long): QuestionCluster?
}
