package com.quno.qunobackend.application.notification.usecase

import com.quno.qunobackend.domain.common.OutboxEvent
import com.quno.qunobackend.domain.common.OutboxEventRepository
import com.quno.qunobackend.domain.common.OutboxEventTypes
import com.quno.qunobackend.domain.notification.Notification
import com.quno.qunobackend.domain.notification.NotificationRepository
import com.quno.qunobackend.domain.watch.WatchRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * Consumes outbox_events (written by Question/Answer use cases, see PLAN.md Phase 2.7) and
 * fans them out as Notifications. Recipients are the question's watchers, plus a per-event-type
 * "always notify" party even if they never explicitly watched — matching the fan-out rules in
 * docs/architecture/domain-model.md and docs/product/vision.md#18-ward:
 *   - QUESTION_REVISION: watchers (the reviser is always the question author, so excluding the
 *     actor is enough)
 *   - NEW_ANSWER: watchers + the question's author
 *   - ANSWER_ACCEPTED: watchers + the accepted answer's author
 *   - REVIEW_REQUESTED: watchers + the question's author (the requester is never the author —
 *     see SelfReviewRequestException)
 *   - REVIEW_RE_REQUESTED: watchers + the original reviewer (the actor is always the question
 *     author — see ReRequestReviewUseCase)
 * The actor who caused the event is never notified about their own action.
 */
@Service
class DispatchOutboxEventsUseCase(
    private val outboxEventRepository: OutboxEventRepository,
    private val watchRepository: WatchRepository,
    private val notificationRepository: NotificationRepository,
) {
    @Transactional
    fun execute(batchSize: Int = 50): Int {
        val events = outboxEventRepository.findUnpublished(batchSize)
        events.forEach { dispatch(it) }
        return events.size
    }

    private fun dispatch(event: OutboxEvent) {
        val questionId = event.aggregateId
        val actorId = extractLong(event.payload, "actorId")
        val answerId = extractLong(event.payload, "answerId")

        val recipients = watchRepository.findWatcherIds(questionId).toMutableSet()
        when (event.eventType) {
            OutboxEventTypes.NEW_ANSWER -> extractLong(event.payload, "questionAuthorId")?.let(recipients::add)
            OutboxEventTypes.ANSWER_ACCEPTED -> extractLong(event.payload, "answerAuthorId")?.let(recipients::add)
            OutboxEventTypes.REVIEW_REQUESTED -> extractLong(event.payload, "questionAuthorId")?.let(recipients::add)
            OutboxEventTypes.REVIEW_RE_REQUESTED -> extractLong(event.payload, "reviewerId")?.let(recipients::add)
        }
        actorId?.let(recipients::remove)

        recipients.forEach { recipientId ->
            notificationRepository.save(
                Notification.create(
                    userId = recipientId,
                    type = event.eventType,
                    questionId = questionId,
                    answerId = answerId,
                    payload = event.payload,
                ),
            )
        }

        outboxEventRepository.markPublished(requireNotNull(event.id))
    }

    private fun extractLong(json: String, field: String): Long? =
        Regex(""""$field"\s*:\s*(\d+)""").find(json)?.groupValues?.get(1)?.toLong()
}
