package com.quno.qunobackend.domain.report

class ReportNotFoundException(id: Long) : RuntimeException("Report not found: $id")

class ReportAlreadyResolvedException(id: Long) : RuntimeException("Report already resolved: $id")

class ModeratorAccessDeniedException(userId: Long) : RuntimeException("Not authorized to moderate: $userId")
