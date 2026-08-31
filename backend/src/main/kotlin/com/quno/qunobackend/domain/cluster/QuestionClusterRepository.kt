package com.quno.qunobackend.domain.cluster

/** Port implemented by infrastructure/persistence/jpa/adapter/QuestionClusterRepositoryAdapter. */
interface QuestionClusterRepository {
    fun save(cluster: QuestionCluster): QuestionCluster
    fun findById(id: Long): QuestionCluster?

    /** Used by Merge (Phase 18, ADR-0030) once every member question has moved to the
     * surviving cluster — the absorbed cluster row no longer serves any purpose. */
    fun delete(id: Long)
}
