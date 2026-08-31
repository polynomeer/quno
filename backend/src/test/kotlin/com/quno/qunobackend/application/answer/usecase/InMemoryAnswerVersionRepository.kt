package com.quno.qunobackend.application.answer.usecase

import com.quno.qunobackend.domain.answer.AnswerVersion
import com.quno.qunobackend.domain.answer.AnswerVersionRepository

class InMemoryAnswerVersionRepository : AnswerVersionRepository {
    private val byId = mutableMapOf<Long, AnswerVersion>()
    private var nextId = 1L

    override fun save(version: AnswerVersion): AnswerVersion {
        val saved = if (version.id == null) {
            AnswerVersion.reconstitute(
                id = nextId++,
                answerId = version.answerId,
                versionNumber = version.versionNumber,
                bodyMarkdown = version.bodyMarkdown,
                createdBy = version.createdBy,
                createdAt = version.createdAt,
            )
        } else {
            version
        }
        byId[requireNotNull(saved.id)] = saved
        return saved
    }

    override fun findById(id: Long): AnswerVersion? = byId[id]

    override fun findByAnswerIdAndVersionNumber(answerId: Long, versionNumber: Int): AnswerVersion? =
        byId.values.find { it.answerId == answerId && it.versionNumber == versionNumber }

    override fun findAllByAnswerIdOrderByVersionNumberAsc(answerId: Long): List<AnswerVersion> =
        byId.values.filter { it.answerId == answerId }.sortedBy { it.versionNumber }
}
