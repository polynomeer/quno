package com.quno.qunobackend.infrastructure.persistence.jpa.adapter

import com.quno.qunobackend.domain.verification.QuestionVerificationRepository
import com.quno.qunobackend.domain.verification.VerificationLevel
import com.quno.qunobackend.domain.verification.VerificationRecord
import com.quno.qunobackend.infrastructure.persistence.jpa.entity.QuestionVerificationJpaEntity
import com.quno.qunobackend.infrastructure.persistence.jpa.repository.QuestionVerificationJpaRepository
import org.springframework.stereotype.Component
import java.time.Instant

@Component
class QuestionVerificationRepositoryAdapter(
    private val jpaRepository: QuestionVerificationJpaRepository,
) : QuestionVerificationRepository {

    override fun find(questionId: Long): VerificationRecord? = jpaRepository.findById(questionId).map { VerificationRecord(it.questionId, it.level, it.lastReproducedAt, it.classification) }.orElse(null)

    override fun save(record: VerificationRecord) {
        jpaRepository.save(QuestionVerificationJpaEntity(record.questionId, record.level, record.lastReproducedAt, record.classification, Instant.now()))
    }

    override fun findVerifiedLevels(questionIds: Collection<Long>, freshSince: Instant): Map<Long, VerificationLevel> {
        if (questionIds.isEmpty()) return emptyMap()
        return jpaRepository.findByQuestionIdInAndLevelInAndLastReproducedAtGreaterThanEqual(questionIds, VERIFIED, freshSince).associate { it.questionId to it.level }
    }

    companion object {
        private val VERIFIED = listOf(VerificationLevel.L2, VerificationLevel.L3, VerificationLevel.L4)
    }
}
