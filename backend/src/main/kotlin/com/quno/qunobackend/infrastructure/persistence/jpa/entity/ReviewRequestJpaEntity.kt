package com.quno.qunobackend.infrastructure.persistence.jpa.entity

import com.quno.qunobackend.domain.review.ReviewRequestStatus
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
@Table(name = "review_requests")
class ReviewRequestJpaEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @Column(name = "question_id", nullable = false)
    val questionId: Long,

    @Column(name = "requested_by", nullable = false)
    val requestedBy: Long,

    @Column(name = "message", nullable = false)
    val message: String,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    val status: ReviewRequestStatus,

    @Column(name = "question_version_number_at_request", nullable = false)
    val questionVersionNumberAtRequest: Int,

    @Column(name = "created_at", nullable = false)
    val createdAt: Instant,

    @Column(name = "addressed_at")
    val addressedAt: Instant?,
)
