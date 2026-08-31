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
 *   - QUESTION_OUTDATED: watchers + the question's author (the actor marking it outdated can be
 *     anyone, including the author themselves — see MarkQuestionOutdatedUseCase)
 *   - NEW_COMMENT: watchers + the question's author, plus the answer's author too when the
 *     comment is on an answer, plus the parent comment's author when this is a reply (all
 *     extracted the same way; absent payload fields are simply skipped by extractLong — see
 *     CreateCommentUseCase)
 *   - CONTENT_HIDDEN: **only** the hidden content's author, never the question's watchers — this
 *     is an administrative notice about one person's content, not an activity signal the rest of
 *     the subscribers care about (see ADR-0028).
 *   - ANSWER_REVISION: watchers + the question's author, same recipients as NEW_ANSWER — editing
 *     an answer's content is exactly the kind of change a Ward subscriber signed up for
 *     (Phase 17, ADR-0029). `questionAuthorId` is simply absent from the payload when the
 *     question itself is gone (e.g. hidden by moderation), so extractLong naturally skips it.
 *   - MENTIONED_IN_COMMENT: **only** the mentioned users, never the question's watchers — this is
 *     a targeted "you were named" notice, not a general activity signal (Phase 19, ADR-0031).
 *     `mentionedUserIds` is a JSON array, parsed by extractLongList rather than extractLong.
 *   - TECH_VERSION_IMPACT_DETECTED: watchers + the question's author, same recipients as
 *     QUESTION_OUTDATED. Unlike every other event type, the actor here is the scheduler, not a
 *     user — the payload has no `actorId`, so nobody is excluded (Phase 21, ADR-0033).
 *   - DIRECT_ASK_REQUESTED: **only** the request's target user, never the question's watchers —
 *     this is a private ask addressed to one specific person, not a public activity signal
 *     (Phase 22, ADR-0034).
 *   - DIRECT_ASK_ACCEPTED / DIRECT_ASK_DECLINED: **only** the original requester — they want to
 *     know the outcome of their own ask, watchers don't.
 * CONTENT_HIDDEN, MENTIONED_IN_COMMENT, and the three DIRECT_ASK_* types are the only event
 * types that skip the base watcher fan-out entirely. The actor who caused the event is never
 * notified about their own action.
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

        val targetedOnlyEventTypes = setOf(
            OutboxEventTypes.CONTENT_HIDDEN,
            OutboxEventTypes.MENTIONED_IN_COMMENT,
            OutboxEventTypes.DIRECT_ASK_REQUESTED,
            OutboxEventTypes.DIRECT_ASK_ACCEPTED,
            OutboxEventTypes.DIRECT_ASK_DECLINED,
        )
        val recipients = if (event.eventType in targetedOnlyEventTypes) {
            mutableSetOf()
        } else {
            watchRepository.findWatcherIds(questionId).toMutableSet()
        }
        when (event.eventType) {
            OutboxEventTypes.NEW_ANSWER -> extractLong(event.payload, "questionAuthorId")?.let(recipients::add)
            OutboxEventTypes.ANSWER_ACCEPTED -> extractLong(event.payload, "answerAuthorId")?.let(recipients::add)
            OutboxEventTypes.REVIEW_REQUESTED -> extractLong(event.payload, "questionAuthorId")?.let(recipients::add)
            OutboxEventTypes.REVIEW_RE_REQUESTED -> extractLong(event.payload, "reviewerId")?.let(recipients::add)
            OutboxEventTypes.QUESTION_OUTDATED -> extractLong(event.payload, "questionAuthorId")?.let(recipients::add)
            OutboxEventTypes.NEW_COMMENT -> {
                extractLong(event.payload, "questionAuthorId")?.let(recipients::add)
                extractLong(event.payload, "answerAuthorId")?.let(recipients::add)
                extractLong(event.payload, "parentCommentAuthorId")?.let(recipients::add)
            }
            OutboxEventTypes.CONTENT_HIDDEN -> extractLong(event.payload, "contentAuthorId")?.let(recipients::add)
            OutboxEventTypes.ANSWER_REVISION -> extractLong(event.payload, "questionAuthorId")?.let(recipients::add)
            OutboxEventTypes.MENTIONED_IN_COMMENT -> extractLongList(event.payload, "mentionedUserIds").forEach(recipients::add)
            OutboxEventTypes.TECH_VERSION_IMPACT_DETECTED -> extractLong(event.payload, "questionAuthorId")?.let(recipients::add)
            OutboxEventTypes.DIRECT_ASK_REQUESTED -> extractLong(event.payload, "targetUserId")?.let(recipients::add)
            OutboxEventTypes.DIRECT_ASK_ACCEPTED, OutboxEventTypes.DIRECT_ASK_DECLINED ->
                extractLong(event.payload, "requesterId")?.let(recipients::add)
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

    private fun extractLongList(json: String, field: String): List<Long> =
        Regex(""""$field"\s*:\s*\[([^\]]*)]""").find(json)
            ?.groupValues?.get(1)
            ?.split(",")
            ?.mapNotNull { it.trim().toLongOrNull() }
            ?: emptyList()
}
