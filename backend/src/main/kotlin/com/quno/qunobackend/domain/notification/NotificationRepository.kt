package com.quno.qunobackend.domain.notification

/** Port implemented by infrastructure/persistence/jpa/adapter/NotificationRepositoryAdapter. */
interface NotificationRepository {
    fun save(notification: Notification): Notification

    /** Most recent first. */
    fun findAllByUserId(userId: Long): List<Notification>
    fun markAllReadForUser(userId: Long)
}
