package com.quno.qunobackend.interfaces.api.directask

import com.quno.qunobackend.application.directask.dto.DirectAskRequestResult
import com.quno.qunobackend.domain.directask.DirectAskRequestStatus
import java.time.Instant

data class DirectAskRequestResponse(
    val id: Long,
    val questionId: Long,
    val requesterId: Long,
    val targetUserId: Long,
    val message: String?,
    val status: DirectAskRequestStatus,
    val createdAt: Instant,
    val respondedAt: Instant?,
)

fun DirectAskRequestResult.toResponse() = DirectAskRequestResponse(
    id = id,
    questionId = questionId,
    requesterId = requesterId,
    targetUserId = targetUserId,
    message = message,
    status = status,
    createdAt = createdAt,
    respondedAt = respondedAt,
)
