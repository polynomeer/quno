package com.quno.qunobackend.domain.directask

import java.time.Instant

enum class DirectAskRequestStatus { PENDING, ACCEPTED, DECLINED }

/**
 * One user asking another specific user to answer a question (Phase 22, ADR-0034) — no payment
 * attached, unlike the "유료 Direct Ask" the original brainstorm floats as a possible future
 * direction. Deliberately not linked to any [com.quno.qunobackend.domain.answer.Answer] — the
 * target simply posts a normal answer through the existing `POST /questions/{id}/answers` flow
 * after accepting.
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
    fun accept(): DirectAskRequest {
        check(status == DirectAskRequestStatus.PENDING) { "direct ask request is already responded to" }
        return DirectAskRequest(id, questionId, requesterId, targetUserId, message, DirectAskRequestStatus.ACCEPTED, createdAt, Instant.now())
    }

    fun decline(): DirectAskRequest {
        check(status == DirectAskRequestStatus.PENDING) { "direct ask request is already responded to" }
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
                status = DirectAskRequestStatus.PENDING,
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
