package com.quno.qunobackend.application.notification.dto

import java.time.Instant

data class NotificationResult(
    val id: Long,
    val type: String,
    val questionId: Long?,
    val answerId: Long?,
    val payload: String,
    val isRead: Boolean,
    val createdAt: Instant,
)
