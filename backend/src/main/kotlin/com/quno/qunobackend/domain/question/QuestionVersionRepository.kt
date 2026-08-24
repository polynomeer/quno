package com.quno.qunobackend.domain.question

/** Port implemented by infrastructure/persistence/jpa/adapter/QuestionVersionRepositoryAdapter. */
interface QuestionVersionRepository {
    fun save(version: QuestionVersion): QuestionVersion
    fun findById(id: Long): QuestionVersion?
    fun findByQuestionIdAndVersionNumber(questionId: Long, versionNumber: Int): QuestionVersion?

    /** Ordered oldest-first (Qv1, Qv2, ...). */
    fun findAllByQuestionIdOrderByVersionNumberAsc(questionId: Long): List<QuestionVersion>
}
