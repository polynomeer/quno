package com.quno.qunobackend.infrastructure.persistence.jpa.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.Instant

@Entity
@Table(name = "question_promotions")
class QuestionPromotionJpaEntity(
    @Id
    @Column(name = "private_question_id")
    val privateQuestionId: Long,

    @Column(name = "public_question_id", nullable = false)
    val publicQuestionId: Long,

    @Column(name = "promoted_by", nullable = false)
    val promotedBy: Long,

    @Column(name = "created_at", nullable = false)
    val createdAt: Instant = Instant.now(),
)
