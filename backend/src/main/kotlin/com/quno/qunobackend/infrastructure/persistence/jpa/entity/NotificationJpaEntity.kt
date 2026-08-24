package com.quno.qunobackend.infrastructure.persistence.jpa.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.Instant

@Entity
@Table(name = "notifications")
class NotificationJpaEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @Column(name = "user_id", nullable = false)
    val userId: Long,

    @Column(nullable = false)
    val type: String,

    @Column(name = "question_id")
    val questionId: Long?,

    @Column(name = "answer_id")
    val answerId: Long?,

    @Column(nullable = false)
    val payload: String,

    @Column(name = "is_read", nullable = false)
    val isRead: Boolean,

    @Column(name = "created_at", nullable = false)
    val createdAt: Instant,
)
