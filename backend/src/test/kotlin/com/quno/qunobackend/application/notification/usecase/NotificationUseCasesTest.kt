package com.quno.qunobackend.application.notification.usecase

import com.quno.qunobackend.domain.notification.Notification
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class NotificationUseCasesTest {
    private val notificationRepository = InMemoryNotificationRepository()
    private val listUseCase = ListMyNotificationsUseCase(notificationRepository)
    private val markAllReadUseCase = MarkAllNotificationsReadUseCase(notificationRepository)

    @Test
    fun `lists only the requesting user's notifications`() {
        notificationRepository.save(Notification.create(userId = 1L, type = "NEW_ANSWER", questionId = 1L, answerId = 5L, payload = "{}"))
        notificationRepository.save(Notification.create(userId = 2L, type = "NEW_ANSWER", questionId = 1L, answerId = 5L, payload = "{}"))

        val result = listUseCase.execute(userId = 1L)

        assertEquals(1, result.size)
    }

    @Test
    fun `mark-read flips only the requesting user's unread notifications`() {
        notificationRepository.save(Notification.create(userId = 1L, type = "NEW_ANSWER", questionId = 1L, answerId = 5L, payload = "{}"))
        notificationRepository.save(Notification.create(userId = 2L, type = "NEW_ANSWER", questionId = 1L, answerId = 5L, payload = "{}"))

        markAllReadUseCase.execute(userId = 1L)

        assertTrue(listUseCase.execute(userId = 1L).all { it.isRead })
        assertTrue(listUseCase.execute(userId = 2L).none { it.isRead })
    }
}
