package com.quno.qunobackend.domain.report

import java.time.Instant

/**
 * Independent side-aggregate, same pattern as Vote/Comment — Question/Answer never hold a
 * reference to this. Duplicate reports on the same target are not merged; "N reports" is a
 * `COUNT(*)` over this table, not a counter field (see ADR-0028).
 */
class Report private constructor(
    val id: Long?,
    val reporterId: Long,
    val targetType: ReportTargetType,
    val targetId: Long,
    val reason: ReportReason,
    val message: String?,
    val status: ReportStatus,
    val resolvedBy: Long?,
    val resolvedAt: Instant?,
    val createdAt: Instant,
) {
    /** Keeps the content as-is, just closes the report. */
    fun dismiss(moderatorId: Long): Report {
        if (status != ReportStatus.PENDING) throw ReportAlreadyResolvedException(requireNotNull(id))
        return Report(id, reporterId, targetType, targetId, reason, message, ReportStatus.DISMISSED, moderatorId, Instant.now(), createdAt)
    }

    /** The use case is responsible for actually hiding the target — this just records the outcome. */
    fun action(moderatorId: Long): Report {
        if (status != ReportStatus.PENDING) throw ReportAlreadyResolvedException(requireNotNull(id))
        return Report(id, reporterId, targetType, targetId, reason, message, ReportStatus.ACTIONED, moderatorId, Instant.now(), createdAt)
    }

    companion object {
        fun file(reporterId: Long, targetType: ReportTargetType, targetId: Long, reason: ReportReason, message: String?): Report {
            val now = Instant.now()
            return Report(
                id = null,
                reporterId = reporterId,
                targetType = targetType,
                targetId = targetId,
                reason = reason,
                message = message,
                status = ReportStatus.PENDING,
                resolvedBy = null,
                resolvedAt = null,
                createdAt = now,
            )
        }

        fun reconstitute(
            id: Long,
            reporterId: Long,
            targetType: ReportTargetType,
            targetId: Long,
            reason: ReportReason,
            message: String?,
            status: ReportStatus,
            resolvedBy: Long?,
            resolvedAt: Instant?,
            createdAt: Instant,
        ): Report = Report(id, reporterId, targetType, targetId, reason, message, status, resolvedBy, resolvedAt, createdAt)
    }
}
