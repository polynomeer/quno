package com.quno.qunobackend.application.notification.usecase

import com.quno.qunobackend.domain.notification.NotificationRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class MarkAllNotificationsReadUseCase(
    private val notificationRepository: NotificationRepository,
) {
    @Transactional
    fun execute(userId: Long) {
        notificationRepository.markAllReadForUser(userId)
    }
}
