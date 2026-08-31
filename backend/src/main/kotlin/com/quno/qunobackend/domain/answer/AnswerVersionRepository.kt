package com.quno.qunobackend.domain.answer

/** Port implemented by infrastructure/persistence/jpa/adapter/AnswerVersionRepositoryAdapter. */
interface AnswerVersionRepository {
    fun save(version: AnswerVersion): AnswerVersion
    fun findById(id: Long): AnswerVersion?
    fun findByAnswerIdAndVersionNumber(answerId: Long, versionNumber: Int): AnswerVersion?

    /** Ordered oldest-first (Av1, Av2, ...). */
    fun findAllByAnswerIdOrderByVersionNumberAsc(answerId: Long): List<AnswerVersion>
}
