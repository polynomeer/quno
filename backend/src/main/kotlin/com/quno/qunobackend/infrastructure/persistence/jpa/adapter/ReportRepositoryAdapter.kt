package com.quno.qunobackend.infrastructure.persistence.jpa.adapter

import com.quno.qunobackend.domain.report.Report
import com.quno.qunobackend.domain.report.ReportRepository
import com.quno.qunobackend.domain.report.ReportStatus
import com.quno.qunobackend.domain.report.ReportTargetType
import com.quno.qunobackend.infrastructure.persistence.jpa.entity.ReportJpaEntity
import com.quno.qunobackend.infrastructure.persistence.jpa.repository.ReportJpaRepository
import org.springframework.stereotype.Component

@Component
class ReportRepositoryAdapter(
    private val jpaRepository: ReportJpaRepository,
) : ReportRepository {

    override fun save(report: Report): Report {
        val entity = ReportJpaEntity(
            id = report.id,
            reporterId = report.reporterId,
            targetType = report.targetType,
            targetId = report.targetId,
            reason = report.reason,
            message = report.message,
            status = report.status,
            resolvedBy = report.resolvedBy,
            resolvedAt = report.resolvedAt,
            createdAt = report.createdAt,
        )
        return jpaRepository.save(entity).toDomain()
    }

    override fun findById(id: Long): Report? = jpaRepository.findById(id).map { it.toDomain() }.orElse(null)

    override fun listByStatus(status: ReportStatus): List<Report> =
        jpaRepository.findAllByStatusOrderByCreatedAtAsc(status).map { it.toDomain() }

    override fun countByTarget(targetType: ReportTargetType, targetId: Long): Long =
        jpaRepository.countByTargetTypeAndTargetId(targetType, targetId)

    private fun ReportJpaEntity.toDomain(): Report = Report.reconstitute(
        id = requireNotNull(id),
        reporterId = reporterId,
        targetType = targetType,
        targetId = targetId,
        reason = reason,
        message = message,
        status = status,
        resolvedBy = resolvedBy,
        resolvedAt = resolvedAt,
        createdAt = createdAt,
    )
}
