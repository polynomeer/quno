package com.quno.qunobackend.infrastructure.persistence.jpa.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.Instant

@Entity
@Table(name = "question_dependencies")
class QuestionDependencyJpaEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @Column(name = "question_id", nullable = false)
    val questionId: Long,

    @Column(name = "version_number", nullable = false)
    val versionNumber: Int,

    @Column(name = "ecosystem", nullable = false)
    val ecosystem: String,

    @Column(name = "name", nullable = false)
    val name: String,

    @Column(name = "version")
    val version: String?,

    @Column(name = "created_at", nullable = false)
    val createdAt: Instant,
)
