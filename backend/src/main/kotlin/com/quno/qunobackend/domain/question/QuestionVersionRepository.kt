package com.quno.qunobackend.domain.question

/** Port implemented by infrastructure/persistence/jpa/adapter/QuestionVersionRepositoryAdapter. */
interface QuestionVersionRepository {
    fun save(version: QuestionVersion): QuestionVersion
    fun findById(id: Long): QuestionVersion?
}
