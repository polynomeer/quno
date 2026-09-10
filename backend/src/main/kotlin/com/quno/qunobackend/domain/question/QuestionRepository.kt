package com.quno.qunobackend.domain.question

/** Port implemented by infrastructure/persistence/jpa/adapter/QuestionRepositoryAdapter. */
interface QuestionRepository {
    fun save(question: Question): Question

    /** Excludes soft-deleted questions. */
    fun findById(id: Long): Question?

    /** Batch form of [findById] — avoids N+1 when hydrating a list of ranked ids (search,
     * related, dashboard, cluster members). Order is not significant; caller re-orders by id. */
    fun findAllByIds(ids: List<Long>): List<Question>

    /** Locks the row (SELECT ... FOR UPDATE) to serialize concurrent revision creation. */
    fun findByIdForUpdate(id: Long): Question?

    /** Most recent first. */
    fun findAllByAuthorId(authorId: Long): List<Question>

    /** Members of a Cluster (PLAN.md 6.1) — order is not significant. */
    fun findAllByClusterId(clusterId: Long): List<Question>

    /** Questions forked from this one (Phase 18, ADR-0030) — order is not significant. */
    fun findAllByOriginQuestionId(originQuestionId: Long): List<Question>
}
