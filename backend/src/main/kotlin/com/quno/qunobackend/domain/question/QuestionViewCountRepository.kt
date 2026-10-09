package com.quno.qunobackend.domain.question

/** Port implemented by infrastructure/persistence/jpa/adapter/QuestionViewCountRepositoryAdapter (ADR-0063). */
interface QuestionViewCountRepository {
    /** Atomic +1 — safe under concurrent views of the same question. */
    fun increment(questionId: Long)

    fun countByQuestionId(questionId: Long): Long

    /** Batched for list views; questions never viewed are absent from the map. */
    fun countsByQuestionIds(questionIds: List<Long>): Map<Long, Long>
}
