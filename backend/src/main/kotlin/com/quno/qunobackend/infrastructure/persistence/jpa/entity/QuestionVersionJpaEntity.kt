package com.quno.qunobackend.infrastructure.persistence.jpa.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.Instant

@Entity
@Table(name = "question_versions")
class QuestionVersionJpaEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @Column(name = "question_id", nullable = false)
    val questionId: Long,

    @Column(name = "version_number", nullable = false)
    val versionNumber: Int,

    @Column(nullable = false)
    val title: String,

    @Column(name = "body_markdown", nullable = false)
    val bodyMarkdown: String,

    @Column
    val environment: String?,

    @Column
    val logs: String?,

    @Column(name = "created_by", nullable = false)
    val createdBy: Long,

    @Column(name = "created_at", nullable = false)
    val createdAt: Instant,
)
