package com.quno.qunobackend.interfaces.api.organization

import com.quno.qunobackend.application.organization.dto.EmailDomainVerificationResult
import com.quno.qunobackend.application.organization.dto.OrganizationResult
import java.time.Instant

data class OrganizationResponse(
    val id: Long,
    val name: String,
    val description: String?,
    val createdBy: Long,
    val memberCount: Long,
    val emailDomain: String?,
    val verified: Boolean,
    val createdAt: Instant,
)

fun OrganizationResult.toResponse() = OrganizationResponse(
    id = id,
    name = name,
    description = description,
    createdBy = createdBy,
    memberCount = memberCount,
    emailDomain = emailDomain,
    verified = verified,
    createdAt = createdAt,
)

data class EmailDomainVerificationResponse(val email: String, val expiresAt: Instant)

fun EmailDomainVerificationResult.toResponse() = EmailDomainVerificationResponse(email = email, expiresAt = expiresAt)
