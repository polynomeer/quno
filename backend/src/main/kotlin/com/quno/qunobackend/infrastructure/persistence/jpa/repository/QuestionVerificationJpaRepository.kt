package com.quno.qunobackend.infrastructure.persistence.jpa.repository

import com.quno.qunobackend.domain.verification.VerificationLevel
import com.quno.qunobackend.infrastructure.persistence.jpa.entity.QuestionVerificationJpaEntity
import org.springframework.data.jpa.repository.JpaRepository
import java.time.Instant

interface QuestionVerificationJpaRepository : JpaRepository<QuestionVerificationJpaEntity, Long> {
    fun findByQuestionIdInAndLevelInAndLastReproducedAtGreaterThanEqual(questionIds: Collection<Long>, levels: Collection<VerificationLevel>, freshSince: Instant): List<QuestionVerificationJpaEntity>
}
