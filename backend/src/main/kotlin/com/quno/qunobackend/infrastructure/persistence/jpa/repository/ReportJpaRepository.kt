package com.quno.qunobackend.infrastructure.persistence.jpa.repository

import com.quno.qunobackend.domain.report.ReportStatus
import com.quno.qunobackend.domain.report.ReportTargetType
import com.quno.qunobackend.infrastructure.persistence.jpa.entity.ReportJpaEntity
import org.springframework.data.jpa.repository.JpaRepository

interface ReportJpaRepository : JpaRepository<ReportJpaEntity, Long> {
    fun findAllByStatusOrderByCreatedAtAsc(status: ReportStatus): List<ReportJpaEntity>
    fun countByTargetTypeAndTargetId(targetType: ReportTargetType, targetId: Long): Long
}
