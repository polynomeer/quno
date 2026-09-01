package com.quno.qunobackend.interfaces.api.directask

import com.quno.qunobackend.application.directask.dto.CreateDirectAskRequestResult
import com.quno.qunobackend.application.directask.dto.DirectAskPaymentResult
import com.quno.qunobackend.application.directask.dto.DirectAskRequestListItemResult
import com.quno.qunobackend.application.directask.dto.DirectAskRequestResult
import com.quno.qunobackend.domain.directask.DirectAskPaymentStatus
import com.quno.qunobackend.domain.directask.DirectAskRequestStatus
import java.time.Instant

data class DirectAskRequestResponse(
    val id: Long,
    val questionId: Long,
    val requesterId: Long,
    val targetUserId: Long,
    val message: String?,
    val status: DirectAskRequestStatus,
    val createdAt: Instant,
    val respondedAt: Instant?,
)

fun DirectAskRequestResult.toResponse() = DirectAskRequestResponse(
    id = id,
    questionId = questionId,
    requesterId = requesterId,
    targetUserId = targetUserId,
    message = message,
    status = status,
    createdAt = createdAt,
    respondedAt = respondedAt,
)

data class DirectAskPaymentResponse(val orderId: String, val amount: Long, val status: DirectAskPaymentStatus, val clientKey: String)

fun DirectAskPaymentResult.toResponse() = DirectAskPaymentResponse(orderId = orderId, amount = amount, status = status, clientKey = clientKey)

data class CreateDirectAskRequestResponse(val request: DirectAskRequestResponse, val payment: DirectAskPaymentResponse)

fun CreateDirectAskRequestResult.toResponse() = CreateDirectAskRequestResponse(request = request.toResponse(), payment = payment.toResponse())

data class DirectAskRequestListItemResponse(
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

fun DirectAskRequestListItemResult.toResponse() = DirectAskRequestListItemResponse(
    id = id,
    questionId = questionId,
    questionTitle = questionTitle,
    requesterId = requesterId,
    requesterNickname = requesterNickname,
    targetUserId = targetUserId,
    targetUserNickname = targetUserNickname,
    message = message,
    status = status,
    createdAt = createdAt,
    respondedAt = respondedAt,
)
