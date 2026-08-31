package com.quno.qunobackend.infrastructure.persistence.jpa.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.Instant

@Entity
@Table(name = "comment_versions")
class CommentVersionJpaEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @Column(name = "comment_id", nullable = false)
    val commentId: Long,

    @Column(name = "version_number", nullable = false)
    val versionNumber: Int,

    @Column(name = "body", nullable = false)
    val body: String,

    @Column(name = "created_at", nullable = false)
    val createdAt: Instant,
)
