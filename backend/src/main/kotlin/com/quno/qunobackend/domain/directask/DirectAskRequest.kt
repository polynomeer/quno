package com.quno.qunobackend.domain.directask

import java.time.Instant

enum class DirectAskRequestStatus { AWAITING_PAYMENT, PENDING, ACCEPTED, DECLINED }

/**
 * One user asking another specific user to answer a question (Phase 22, ADR-0034), now paid
 * (Phase 25, ADR-0037, superseding Phase 22's free flow) — asking costs a flat fee
 * (`quno.direct-ask.fee-amount`), refunded automatically if the target declines. Deliberately
 * not linked to any [com.quno.qunobackend.domain.answer.Answer] — the target simply posts a
 * normal answer through the existing `POST /questions/{id}/answers` flow after accepting.
 *
 * Starts `AWAITING_PAYMENT` rather than `PENDING` — the target is never notified, and the
 * request doesn't count as "open" for [com.quno.qunobackend.domain.directask.DirectAskPayment]
 * duplicate-prevention purposes, until [activate] confirms payment actually went through. This
 * stops a requester from spamming unpaid requests.
 */
class DirectAskRequest private constructor(
    val id: Long?,
    val questionId: Long,
    val requesterId: Long,
    val targetUserId: Long,
    val message: String?,
    val status: DirectAskRequestStatus,
    val createdAt: Instant,
    val respondedAt: Instant?,
) {
    /** Payment confirmed — the target can now see and respond to this request. */
    fun activate(): DirectAskRequest {
        check(status == DirectAskRequestStatus.AWAITING_PAYMENT) { "direct ask request is not awaiting payment" }
        return DirectAskRequest(id, questionId, requesterId, targetUserId, message, DirectAskRequestStatus.PENDING, createdAt, respondedAt)
    }

    fun accept(): DirectAskRequest {
        check(status == DirectAskRequestStatus.PENDING) { "direct ask request is not open" }
        return DirectAskRequest(id, questionId, requesterId, targetUserId, message, DirectAskRequestStatus.ACCEPTED, createdAt, Instant.now())
    }

    fun decline(): DirectAskRequest {
        check(status == DirectAskRequestStatus.PENDING) { "direct ask request is not open" }
        return DirectAskRequest(id, questionId, requesterId, targetUserId, message, DirectAskRequestStatus.DECLINED, createdAt, Instant.now())
    }

    companion object {
        fun request(questionId: Long, requesterId: Long, targetUserId: Long, message: String?): DirectAskRequest =
            DirectAskRequest(
                id = null,
                questionId = questionId,
                requesterId = requesterId,
                targetUserId = targetUserId,
                message = message?.trim()?.ifBlank { null },
                status = DirectAskRequestStatus.AWAITING_PAYMENT,
                createdAt = Instant.now(),
                respondedAt = null,
            )

        fun reconstitute(
            id: Long,
            questionId: Long,
            requesterId: Long,
            targetUserId: Long,
            message: String?,
            status: DirectAskRequestStatus,
            createdAt: Instant,
            respondedAt: Instant?,
        ): DirectAskRequest = DirectAskRequest(id, questionId, requesterId, targetUserId, message, status, createdAt, respondedAt)
    }
}
