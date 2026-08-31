package com.quno.qunobackend.infrastructure.persistence.jpa.adapter

import com.quno.qunobackend.domain.answer.AnswerVersion
import com.quno.qunobackend.domain.answer.AnswerVersionRepository
import com.quno.qunobackend.infrastructure.persistence.jpa.entity.AnswerVersionJpaEntity
import com.quno.qunobackend.infrastructure.persistence.jpa.repository.AnswerVersionJpaRepository
import org.springframework.stereotype.Component

@Component
class AnswerVersionRepositoryAdapter(
    private val jpaRepository: AnswerVersionJpaRepository,
) : AnswerVersionRepository {

    override fun save(version: AnswerVersion): AnswerVersion {
        val entity = AnswerVersionJpaEntity(
            id = version.id,
            answerId = version.answerId,
            versionNumber = version.versionNumber,
            bodyMarkdown = version.bodyMarkdown,
            createdBy = version.createdBy,
            createdAt = version.createdAt,
        )
        return jpaRepository.save(entity).toDomain()
    }

    override fun findById(id: Long): AnswerVersion? = jpaRepository.findById(id).orElse(null)?.toDomain()

    override fun findByAnswerIdAndVersionNumber(answerId: Long, versionNumber: Int): AnswerVersion? =
        jpaRepository.findByAnswerIdAndVersionNumber(answerId, versionNumber)?.toDomain()

    override fun findAllByAnswerIdOrderByVersionNumberAsc(answerId: Long): List<AnswerVersion> =
        jpaRepository.findAllByAnswerIdOrderByVersionNumberAsc(answerId).map { it.toDomain() }

    private fun AnswerVersionJpaEntity.toDomain(): AnswerVersion = AnswerVersion.reconstitute(
        id = requireNotNull(id),
        answerId = answerId,
        versionNumber = versionNumber,
        bodyMarkdown = bodyMarkdown,
        createdBy = createdBy,
        createdAt = createdAt,
    )
}
