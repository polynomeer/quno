package com.quno.qunobackend.infrastructure.persistence.jpa.entity

import com.quno.qunobackend.domain.reproduction.BisectConfidence
import com.quno.qunobackend.domain.reproduction.BisectDecision
import com.quno.qunobackend.domain.reproduction.BisectStatus
import com.quno.qunobackend.domain.reproduction.ReproductionResult
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
@Table(name = "reproduction_bisects")
class ReproductionBisectJpaEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @Column(name = "capsule_id", nullable = false)
    val capsuleId: Long,

    @Column(name = "from_version", nullable = false)
    val fromVersion: String,

    @Column(name = "to_version", nullable = false)
    val toVersion: String,

    @Enumerated(EnumType.STRING)
    @Column(name = "from_result", nullable = false)
    val fromResult: ReproductionResult,

    @Enumerated(EnumType.STRING)
    @Column(name = "to_result", nullable = false)
    val toResult: ReproductionResult,

    /** Comma-separated; versions never contain commas. */
    @Column(nullable = false)
    val candidates: String,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    val status: BisectStatus,

    @Column(name = "low_version", nullable = false)
    val lowVersion: String,

    @Column(name = "high_version", nullable = false)
    val highVersion: String,

    @Enumerated(EnumType.STRING)
    @Column
    val confidence: BisectConfidence?,

    @Column(name = "runs_used", nullable = false)
    val runsUsed: Int,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    val decision: BisectDecision,

    @Column(name = "created_by", nullable = false)
    val createdBy: Long,

    @Column(name = "created_at", nullable = false)
    val createdAt: Instant,

    @Column(name = "finished_at")
    val finishedAt: Instant?,
)
