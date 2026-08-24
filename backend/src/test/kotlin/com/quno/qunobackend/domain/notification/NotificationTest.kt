package com.quno.qunobackend.domain.notification

import org.junit.jupiter.api.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class NotificationTest {

    @Test
    fun `create starts unread`() {
        val notification = Notification.create(userId = 1L, type = "NEW_ANSWER", questionId = 1L, answerId = 5L, payload = "{}")

        assertFalse(notification.isRead)
    }

    @Test
    fun `markRead flips the flag`() {
        val notification = Notification.create(userId = 1L, type = "NEW_ANSWER", questionId = 1L, answerId = 5L, payload = "{}")

        assertTrue(notification.markRead().isRead)
    }
}
