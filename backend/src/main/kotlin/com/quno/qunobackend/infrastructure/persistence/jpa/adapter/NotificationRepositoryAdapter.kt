package com.quno.qunobackend.infrastructure.persistence.jpa.adapter

import com.quno.qunobackend.domain.notification.Notification
import com.quno.qunobackend.domain.notification.NotificationRepository
import com.quno.qunobackend.infrastructure.persistence.jpa.entity.NotificationJpaEntity
import com.quno.qunobackend.infrastructure.persistence.jpa.repository.NotificationJpaRepository
import org.springframework.stereotype.Component

@Component
class NotificationRepositoryAdapter(
    private val jpaRepository: NotificationJpaRepository,
) : NotificationRepository {

    override fun save(notification: Notification): Notification {
        val entity = NotificationJpaEntity(
            id = notification.id,
            userId = notification.userId,
            type = notification.type,
            questionId = notification.questionId,
            answerId = notification.answerId,
            payload = notification.payload,
            isRead = notification.isRead,
            createdAt = notification.createdAt,
        )
        return jpaRepository.save(entity).toDomain()
    }

    override fun findAllByUserId(userId: Long): List<Notification> =
        jpaRepository.findAllByUserIdOrderByCreatedAtDesc(userId).map { it.toDomain() }

    override fun markAllReadForUser(userId: Long) {
        jpaRepository.markAllReadForUser(userId)
    }

    private fun NotificationJpaEntity.toDomain(): Notification = Notification.reconstitute(
        id = requireNotNull(id),
        userId = userId,
        type = type,
        questionId = questionId,
        answerId = answerId,
        payload = payload,
        isRead = isRead,
        createdAt = createdAt,
    )
}
