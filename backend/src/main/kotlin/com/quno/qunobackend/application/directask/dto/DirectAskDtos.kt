package com.quno.qunobackend.application.directask.dto

import com.quno.qunobackend.domain.directask.DirectAskPaymentStatus
import com.quno.qunobackend.domain.directask.DirectAskRequestStatus
import java.time.Instant

data class CreateDirectAskRequestCommand(
    val questionId: Long,
    val requesterId: Long,
    val targetUserId: Long,
    val message: String?,
)

data class DirectAskRequestResult(
    val id: Long,
    val questionId: Long,
    val requesterId: Long,
    val targetUserId: Long,
    val message: String?,
    val status: DirectAskRequestStatus,
    val createdAt: Instant,
    val respondedAt: Instant?,
)

/** [clientKey] is Toss's public widget key (not secret) — the frontend needs it to render the
 * payment widget for [orderId]/[amount]. */
data class DirectAskPaymentResult(
    val orderId: String,
    val amount: Long,
    val status: DirectAskPaymentStatus,
    val clientKey: String,
)

data class CreateDirectAskRequestResult(val request: DirectAskRequestResult, val payment: DirectAskPaymentResult)

data class ConfirmDirectAskPaymentCommand(val orderId: String, val paymentKey: String, val amount: Long)

/** Denormalized for `GET /me/direct-asks` only (mirrors SavedQuestionResult) — the frontend list
 * screen needs a title and nicknames to render, not just ids. */
data class DirectAskRequestListItemResult(
    val id: Long,
    val questionId: Long,
    val questionTitle: String,
    val requesterId: Long,
    val requesterNickname: String,
    val targetUserId: Long,
    val targetUserNickname: String,
    val message: String?,
    val status: DirectAskRequestStatus,
    val createdAt: Instant,
    val respondedAt: Instant?,
)
