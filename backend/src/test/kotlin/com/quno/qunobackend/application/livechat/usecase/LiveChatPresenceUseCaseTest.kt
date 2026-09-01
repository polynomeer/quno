package com.quno.qunobackend.application.livechat.usecase

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class LiveChatPresenceUseCaseTest {
    private val tracker = FakeLiveChatPresenceTracker()
    private val useCase = LiveChatPresenceUseCase(tracker)

    @Test
    fun `joining increases the viewer count, leaving decreases it`() {
        useCase.join(1L, "session-a")
        useCase.join(1L, "session-b")
        assertEquals(2L, useCase.countViewers(1L))

        useCase.leave(1L, "session-a")
        assertEquals(1L, useCase.countViewers(1L))
    }

    @Test
    fun `viewers are tracked per question independently`() {
        useCase.join(1L, "session-a")
        useCase.join(2L, "session-a")

        assertEquals(1L, useCase.countViewers(1L))
        assertEquals(1L, useCase.countViewers(2L))
    }
}
