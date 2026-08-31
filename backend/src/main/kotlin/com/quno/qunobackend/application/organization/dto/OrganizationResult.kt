package com.quno.qunobackend.application.organization.dto

import java.time.Instant

data class OrganizationResult(
    val id: Long,
    val name: String,
    val description: String?,
    val createdBy: Long,
    val memberCount: Long,
    val createdAt: Instant,
)
