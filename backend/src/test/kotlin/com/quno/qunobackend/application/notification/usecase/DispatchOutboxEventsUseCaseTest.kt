package com.quno.qunobackend.application.notification.usecase

import com.quno.qunobackend.application.common.InMemoryOutboxEventRepository
import com.quno.qunobackend.application.watch.usecase.InMemoryWatchRepository
import com.quno.qunobackend.domain.common.OutboxEvent
import com.quno.qunobackend.domain.common.OutboxEventTypes
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class DispatchOutboxEventsUseCaseTest {
    private val outboxEventRepository = InMemoryOutboxEventRepository()
    private val watchRepository = InMemoryWatchRepository()
    private val notificationRepository = InMemoryNotificationRepository()
    private val useCase = DispatchOutboxEventsUseCase(outboxEventRepository, watchRepository, notificationRepository)

    @Test
    fun `QUESTION_REVISION notifies watchers but not the reviser`() {
        watchRepository.watch(userId = 10L, questionId = 1L)
        watchRepository.watch(userId = 20L, questionId = 1L)
        outboxEventRepository.save(
            OutboxEvent.create(
                eventType = OutboxEventTypes.QUESTION_REVISION,
                aggregateType = "QUESTION",
                aggregateId = 1L,
                payload = """{"versionNumber":2,"actorId":10}""",
            ),
        )

        useCase.execute()

        assertEquals(listOf(20L), notificationRepository.findAllByUserId(20L).map { it.userId })
        assertTrue(notificationRepository.findAllByUserId(10L).isEmpty())
    }

    @Test
    fun `NEW_ANSWER also notifies the question author even if they never watched`() {
        watchRepository.watch(userId = 20L, questionId = 1L)
        outboxEventRepository.save(
            OutboxEvent.create(
                eventType = OutboxEventTypes.NEW_ANSWER,
                aggregateType = "QUESTION",
                aggregateId = 1L,
                payload = """{"answerId":5,"actorId":30,"questionAuthorId":99}""",
            ),
        )

        useCase.execute()

        assertTrue(notificationRepository.findAllByUserId(20L).isNotEmpty())
        assertTrue(notificationRepository.findAllByUserId(99L).isNotEmpty())
        assertTrue(notificationRepository.findAllByUserId(30L).isEmpty())
    }

    @Test
    fun `ANSWER_ACCEPTED also notifies the answer's author`() {
        outboxEventRepository.save(
            OutboxEvent.create(
                eventType = OutboxEventTypes.ANSWER_ACCEPTED,
                aggregateType = "QUESTION",
                aggregateId = 1L,
                payload = """{"answerId":5,"actorId":99,"answerAuthorId":30}""",
            ),
        )

        useCase.execute()

        val notifications = notificationRepository.findAllByUserId(30L)
        assertEquals(1, notifications.size)
        assertEquals(OutboxEventTypes.ANSWER_ACCEPTED, notifications.single().type)
    }

    @Test
    fun `CONTENT_HIDDEN notifies only the hidden content's author, not watchers`() {
        watchRepository.watch(userId = 20L, questionId = 1L)
        outboxEventRepository.save(
            OutboxEvent.create(
                eventType = OutboxEventTypes.CONTENT_HIDDEN,
                aggregateType = "QUESTION",
                aggregateId = 1L,
                payload = """{"actorId":99,"contentAuthorId":30}""",
            ),
        )

        useCase.execute()

        assertTrue(notificationRepository.findAllByUserId(20L).isEmpty())
        assertEquals(1, notificationRepository.findAllByUserId(30L).size)
    }

    @Test
    fun `ANSWER_REVISION also notifies the question author, same as NEW_ANSWER`() {
        watchRepository.watch(userId = 20L, questionId = 1L)
        outboxEventRepository.save(
            OutboxEvent.create(
                eventType = OutboxEventTypes.ANSWER_REVISION,
                aggregateType = "QUESTION",
                aggregateId = 1L,
                payload = """{"answerId":5,"versionNumber":2,"actorId":30,"questionAuthorId":99}""",
            ),
        )

        useCase.execute()

        assertTrue(notificationRepository.findAllByUserId(20L).isNotEmpty())
        assertTrue(notificationRepository.findAllByUserId(99L).isNotEmpty())
        assertTrue(notificationRepository.findAllByUserId(30L).isEmpty())
    }

    @Test
    fun `marks the event published so it is not dispatched twice`() {
        outboxEventRepository.save(
            OutboxEvent.create(
                eventType = OutboxEventTypes.QUESTION_REVISION,
                aggregateType = "QUESTION",
                aggregateId = 1L,
                payload = """{"versionNumber":2,"actorId":10}""",
            ),
        )
        watchRepository.watch(userId = 20L, questionId = 1L)

        useCase.execute()
        useCase.execute()

        assertEquals(1, notificationRepository.findAllByUserId(20L).size)
        assertFalse(outboxEventRepository.events.any { it.publishedAt == null })
    }
}
