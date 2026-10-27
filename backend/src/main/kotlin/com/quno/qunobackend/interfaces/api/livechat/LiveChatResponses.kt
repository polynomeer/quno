package com.quno.qunobackend.interfaces.api.livechat

import com.quno.qunobackend.application.livechat.dto.LiveChatAnswerDraftResult
import com.quno.qunobackend.application.livechat.dto.LiveChatMessageResult
import com.quno.qunobackend.application.livechat.dto.LiveChatRoomResult
import jakarta.validation.constraints.NotEmpty
import jakarta.validation.constraints.Size
import java.time.Instant

data class LiveChatRoomResponse(val id: Long, val questionId: Long, val createdBy: Long, val createdAt: Instant)

fun LiveChatRoomResult.toResponse() = LiveChatRoomResponse(id = id, questionId = questionId, createdBy = createdBy, createdAt = createdAt)

data class LiveChatMessageResponse(
    val id: String,
    val roomId: Long,
    val senderId: Long,
    /** Filled by the history endpoint (ADR-0064 pattern); null where not enriched. */
    val senderNickname: String?,
    val body: String,
    val createdAt: Instant,
)

fun LiveChatMessageResult.toResponse(senderNickname: String? = null) = LiveChatMessageResponse(
    id = id,
    roomId = roomId,
    senderId = senderId,
    senderNickname = senderNickname,
    body = body,
    createdAt = createdAt,
)

data class LiveChatAnswerDraftRequest(
    @field:NotEmpty
    @field:Size(max = 50)
    val messageIds: List<String>,
)

data class LiveChatAnswerDraftResponse(val roomId: Long, val questionId: Long, val body: String, val messageCount: Int)

fun LiveChatAnswerDraftResult.toResponse() = LiveChatAnswerDraftResponse(roomId = roomId, questionId = questionId, body = body, messageCount = messageCount)
