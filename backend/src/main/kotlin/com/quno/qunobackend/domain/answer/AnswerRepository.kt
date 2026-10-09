package com.quno.qunobackend.domain.answer

/** Port implemented by infrastructure/persistence/jpa/adapter/AnswerRepositoryAdapter. */
interface AnswerRepository {
    fun save(answer: Answer): Answer

    /** Excludes soft-deleted answers. */
    fun findById(id: Long): Answer?
    fun findAllByQuestionId(questionId: Long): List<Answer>

    /** At most one per question, enforced by the accept use case. */
    fun findAcceptedByQuestionId(questionId: Long): Answer?

    /** Most recent first. */
    fun findAllByAuthorId(authorId: Long): List<Answer>

    /** Non-deleted answer count per question, batched for list views (ADR-0062). Questions
     * with no answers are absent from the map. */
    fun countByQuestionIds(questionIds: List<Long>): Map<Long, Int>
}
