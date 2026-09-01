package com.quno.qunobackend.infrastructure.persistence.jpa.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.Instant

@Entity
@Table(name = "tags")
class TagJpaEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @Column(nullable = false)
    val name: String,

    @Column(nullable = false)
    val slug: String,

    @Column
    val description: String?,

    @Column(name = "docs_url")
    val docsUrl: String?,

    @Column(name = "deleted_at")
    val deletedAt: Instant?,

    @Column(name = "created_at", nullable = false)
    val createdAt: Instant,
)
