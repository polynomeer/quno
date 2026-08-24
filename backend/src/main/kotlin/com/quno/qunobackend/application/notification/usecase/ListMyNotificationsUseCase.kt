package com.quno.qunobackend.application.notification.usecase

import com.quno.qunobackend.application.notification.dto.NotificationResult
import com.quno.qunobackend.domain.notification.Notification
import com.quno.qunobackend.domain.notification.NotificationRepository
import org.springframework.stereotype.Service

@Service
class ListMyNotificationsUseCase(
    private val notificationRepository: NotificationRepository,
) {
    fun execute(userId: Long): List<NotificationResult> =
        notificationRepository.findAllByUserId(userId).map { it.toResult() }
}

private fun Notification.toResult() = NotificationResult(
    id = requireNotNull(id),
    type = type,
    questionId = questionId,
    answerId = answerId,
    payload = payload,
    isRead = isRead,
    createdAt = createdAt,
)
