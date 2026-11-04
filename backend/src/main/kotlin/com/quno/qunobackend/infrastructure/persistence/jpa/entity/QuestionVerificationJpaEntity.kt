package com.quno.qunobackend.infrastructure.persistence.jpa.entity

import com.quno.qunobackend.domain.verification.QuestionClassification
import com.quno.qunobackend.domain.verification.VerificationLevel
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.Instant

@Entity
@Table(name = "question_verifications")
class QuestionVerificationJpaEntity(
    @Id
    @Column(name = "question_id")
    val questionId: Long,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    val level: VerificationLevel,

    @Column(name = "last_reproduced_at")
    val lastReproducedAt: Instant?,

    @Enumerated(EnumType.STRING)
    @Column
    val classification: QuestionClassification?,

    @Column(name = "updated_at", nullable = false)
    val updatedAt: Instant,
)
