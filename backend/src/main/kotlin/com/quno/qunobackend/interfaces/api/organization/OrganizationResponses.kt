package com.quno.qunobackend.interfaces.api.organization

import com.quno.qunobackend.application.organization.dto.OrganizationResult
import java.time.Instant

data class OrganizationResponse(
    val id: Long,
    val name: String,
    val description: String?,
    val createdBy: Long,
    val memberCount: Long,
    val createdAt: Instant,
)

fun OrganizationResult.toResponse() = OrganizationResponse(
    id = id,
    name = name,
    description = description,
    createdBy = createdBy,
    memberCount = memberCount,
    createdAt = createdAt,
)
