package com.quno.qunobackend.domain.answer

/** Port implemented by infrastructure/persistence/jpa/adapter/AnswerRepositoryAdapter. */
interface AnswerRepository {
    fun save(answer: Answer): Answer

    /** Excludes soft-deleted answers. */
    fun findById(id: Long): Answer?
    fun findAllByQuestionId(questionId: Long): List<Answer>

    /** At most one per question, enforced by the accept use case. */
    fun findAcceptedByQuestionId(questionId: Long): Answer?
}
