package com.quno.qunobackend.application.notification.usecase

import com.quno.qunobackend.domain.notification.Notification
import com.quno.qunobackend.domain.notification.NotificationRepository

class InMemoryNotificationRepository : NotificationRepository {
    private val byId = mutableMapOf<Long, Notification>()
    private var nextId = 1L

    override fun save(notification: Notification): Notification {
        val saved = if (notification.id == null) {
            Notification.reconstitute(
                id = nextId++,
                userId = notification.userId,
                type = notification.type,
                questionId = notification.questionId,
                answerId = notification.answerId,
                payload = notification.payload,
                isRead = notification.isRead,
                createdAt = notification.createdAt,
            )
        } else {
            notification
        }
        byId[requireNotNull(saved.id)] = saved
        return saved
    }

    override fun findAllByUserId(userId: Long): List<Notification> =
        byId.values.filter { it.userId == userId }.sortedByDescending { it.createdAt }

    override fun markAllReadForUser(userId: Long) {
        byId.values.filter { it.userId == userId && !it.isRead }.forEach { save(it.markRead()) }
    }
}
