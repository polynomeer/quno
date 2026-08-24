package com.quno.qunobackend.domain.notification

import java.time.Instant

/** Fan-out target of a Ward(Watch) subscription — see domain/watch/WatchRepository. */
class Notification private constructor(
    val id: Long?,
    val userId: Long,
    val type: String,
    val questionId: Long?,
    val answerId: Long?,
    val payload: String,
    val isRead: Boolean,
    val createdAt: Instant,
) {
    fun markRead(): Notification =
        Notification(id, userId, type, questionId, answerId, payload, true, createdAt)

    companion object {
        fun create(userId: Long, type: String, questionId: Long?, answerId: Long?, payload: String): Notification =
            Notification(
                id = null,
                userId = userId,
                type = type,
                questionId = questionId,
                answerId = answerId,
                payload = payload,
                isRead = false,
                createdAt = Instant.now(),
            )

        fun reconstitute(
            id: Long,
            userId: Long,
            type: String,
            questionId: Long?,
            answerId: Long?,
            payload: String,
            isRead: Boolean,
            createdAt: Instant,
        ): Notification = Notification(id, userId, type, questionId, answerId, payload, isRead, createdAt)
    }
}
