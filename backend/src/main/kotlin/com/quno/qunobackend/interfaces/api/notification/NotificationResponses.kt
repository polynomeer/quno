package com.quno.qunobackend.interfaces.api.notification

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
