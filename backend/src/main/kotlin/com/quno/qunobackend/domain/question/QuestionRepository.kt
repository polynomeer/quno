package com.quno.qunobackend.domain.question

/** Port implemented by infrastructure/persistence/jpa/adapter/QuestionRepositoryAdapter. */
interface QuestionRepository {
    fun save(question: Question): Question

    /** Excludes soft-deleted questions. */
    fun findById(id: Long): Question?

    /** Locks the row (SELECT ... FOR UPDATE) to serialize concurrent revision creation. */
    fun findByIdForUpdate(id: Long): Question?

    /** Most recent first. */
    fun findAllByAuthorId(authorId: Long): List<Question>

    /** Members of a Cluster (PLAN.md 6.1) — order is not significant. */
    fun findAllByClusterId(clusterId: Long): List<Question>
}
