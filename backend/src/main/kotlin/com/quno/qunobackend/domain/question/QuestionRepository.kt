package com.quno.qunobackend.domain.question

/** Port implemented by infrastructure/persistence/jpa/adapter/QuestionRepositoryAdapter. */
interface QuestionRepository {
    fun save(question: Question): Question

    /** Excludes soft-deleted questions. */
    fun findById(id: Long): Question?
}
