package com.quno.qunobackend.infrastructure.persistence.jpa.entity

import com.quno.qunobackend.domain.report.ReportReason
import com.quno.qunobackend.domain.report.ReportStatus
import com.quno.qunobackend.domain.report.ReportTargetType
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
@Table(name = "reports")
class ReportJpaEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @Column(name = "reporter_id", nullable = false)
    val reporterId: Long,

    @Column(name = "target_type")
    @Enumerated(EnumType.STRING)
    val targetType: ReportTargetType,

    @Column(name = "target_id", nullable = false)
    val targetId: Long,

    @Column(name = "reason")
    @Enumerated(EnumType.STRING)
    val reason: ReportReason,

    @Column(name = "message")
    val message: String?,

    @Column(name = "status")
    @Enumerated(EnumType.STRING)
    val status: ReportStatus,

    @Column(name = "resolved_by")
    val resolvedBy: Long?,

    @Column(name = "resolved_at")
    val resolvedAt: Instant?,

    @Column(name = "created_at", nullable = false)
    val createdAt: Instant,
)
