package com.quno.qunobackend.infrastructure.persistence.jpa.adapter

import com.quno.qunobackend.domain.stack.AnswerApplicabilityRepository
import com.quno.qunobackend.domain.stack.ApplicabilityRange
import com.quno.qunobackend.domain.stack.Ecosystem
import com.quno.qunobackend.infrastructure.persistence.jpa.entity.AnswerApplicabilityRangeJpaEntity
import com.quno.qunobackend.infrastructure.persistence.jpa.repository.AnswerApplicabilityRangeJpaRepository
import org.springframework.stereotype.Component
import java.time.Instant

@Component
class AnswerApplicabilityRepositoryAdapter(
    private val jpaRepository: AnswerApplicabilityRangeJpaRepository,
) : AnswerApplicabilityRepository {

    override fun replaceForAnswer(answerId: Long, questionId: Long, versionNumber: Int, ranges: List<ApplicabilityRange>) {
        jpaRepository.deleteAllByAnswerId(answerId)
        val now = Instant.now()
        jpaRepository.saveAll(
            ranges.map {
                AnswerApplicabilityRangeJpaEntity(
                    answerId = answerId,
                    questionId = questionId,
                    versionNumber = versionNumber,
                    ecosystem = it.ecosystem.name,
                    name = it.name,
                    fromVersion = it.fromVersion,
                    untilVersion = it.untilVersion,
                    createdAt = now,
                )
            },
        )
    }

    override fun findByAnswerIds(answerIds: Collection<Long>): Map<Long, List<ApplicabilityRange>> {
        if (answerIds.isEmpty()) return emptyMap()
        return jpaRepository.findAllByAnswerIdInOrderByIdAsc(answerIds).groupBy({ it.answerId }, { it.toDomain() })
    }

    override fun findByQuestionIds(questionIds: Collection<Long>): Map<Long, List<ApplicabilityRange>> {
        if (questionIds.isEmpty()) return emptyMap()
        return jpaRepository.findAllByQuestionIdInOrderByIdAsc(questionIds).groupBy({ it.questionId }, { it.toDomain() })
    }

    private fun AnswerApplicabilityRangeJpaEntity.toDomain() = ApplicabilityRange(Ecosystem.valueOf(ecosystem), name, fromVersion, untilVersion)
}
