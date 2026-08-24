package com.quno.qunobackend.infrastructure.persistence.jpa.entity

import com.quno.qunobackend.domain.question.QuestionStatus
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.Instant

@Entity
@Table(name = "questions")
class QuestionJpaEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @Column(name = "author_id", nullable = false)
    val authorId: Long,

    @Column(nullable = false)
    val title: String,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    val status: QuestionStatus,

    @Column(name = "latest_version_id")
    val latestVersionId: Long?,

    @Column(name = "accepted_answer_id")
    val acceptedAnswerId: Long?,

    @Column(name = "deleted_at")
    val deletedAt: Instant?,

    @Column(name = "created_at", nullable = false)
    val createdAt: Instant,

    @Column(name = "updated_at", nullable = false)
    val updatedAt: Instant,
)
