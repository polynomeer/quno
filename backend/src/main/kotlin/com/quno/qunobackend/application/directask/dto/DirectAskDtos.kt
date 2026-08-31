package com.quno.qunobackend.application.directask.dto

import com.quno.qunobackend.domain.directask.DirectAskRequestStatus
import java.time.Instant

data class CreateDirectAskRequestCommand(
    val questionId: Long,
    val requesterId: Long,
    val targetUserId: Long,
    val message: String?,
)

data class DirectAskRequestResult(
    val id: Long,
    val questionId: Long,
    val requesterId: Long,
    val targetUserId: Long,
    val message: String?,
    val status: DirectAskRequestStatus,
    val createdAt: Instant,
    val respondedAt: Instant?,
)
