package com.quno.qunobackend.interfaces.api.notification

import com.quno.qunobackend.application.notification.dto.NotificationResult
import java.time.Instant

data class NotificationResponse(
    val id: Long,
    val type: String,
    val questionId: Long?,
    val answerId: Long?,
    val payload: String,
    val isRead: Boolean,
    val createdAt: Instant,
)

fun NotificationResult.toResponse() = NotificationResponse(
    id = id,
    type = type,
    questionId = questionId,
    answerId = answerId,
    payload = payload,
    isRead = isRead,
    createdAt = createdAt,
)
