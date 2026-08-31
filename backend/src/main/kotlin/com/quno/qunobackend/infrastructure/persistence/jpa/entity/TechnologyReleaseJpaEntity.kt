package com.quno.qunobackend.infrastructure.persistence.jpa.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.Instant
import java.time.LocalDate

@Entity
@Table(name = "technology_releases")
class TechnologyReleaseJpaEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @Column(name = "tag_slug", nullable = false)
    val tagSlug: String,

    @Column(name = "product_slug", nullable = false)
    val productSlug: String,

    @Column(name = "latest_version", nullable = false)
    val latestVersion: String,

    @Column(name = "latest_release_date", nullable = false)
    val latestReleaseDate: LocalDate,

    @Column(name = "checked_at", nullable = false)
    val checkedAt: Instant,

    @Column(name = "updated_at", nullable = false)
    val updatedAt: Instant,
)
