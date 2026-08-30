package com.quno.qunobackend.infrastructure.persistence.jpa.entity

import com.quno.qunobackend.domain.comment.CommentTargetType
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
@Table(name = "comments")
class CommentJpaEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @Column(name = "target_type")
    @Enumerated(EnumType.STRING)
    val targetType: CommentTargetType,

    @Column(name = "target_id", nullable = false)
    val targetId: Long,

    @Column(name = "author_id", nullable = false)
    val authorId: Long,

    @Column(name = "body", nullable = false)
    val body: String,

    @Column(name = "deleted_at")
    val deletedAt: Instant?,

    @Column(name = "created_at", nullable = false)
    val createdAt: Instant,

    @Column(name = "updated_at", nullable = false)
    val updatedAt: Instant,
)
