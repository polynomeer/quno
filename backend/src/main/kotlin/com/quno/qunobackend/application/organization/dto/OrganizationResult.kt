package com.quno.qunobackend.application.organization.dto

import java.time.Instant

data class OrganizationResult(
    val id: Long,
    val name: String,
    val description: String?,
    val createdBy: Long,
    val memberCount: Long,
    /** Non-null only for a real company/school confirmed via email verification (Phase 23, ADR-0035). */
    val emailDomain: String?,
    val verified: Boolean,
    val createdAt: Instant,
)
