package com.quno.qunobackend.application.organization.dto

import java.time.Instant

data class EmailDomainVerificationResult(val email: String, val expiresAt: Instant)
