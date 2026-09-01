package com.quno.qunobackend.interfaces.api.livechat

import com.quno.qunobackend.application.livechat.dto.LiveChatMessageResult
import com.quno.qunobackend.application.livechat.dto.LiveChatRoomResult
import java.time.Instant

data class LiveChatRoomResponse(val id: Long, val questionId: Long, val createdBy: Long, val createdAt: Instant)

fun LiveChatRoomResult.toResponse() = LiveChatRoomResponse(id = id, questionId = questionId, createdBy = createdBy, createdAt = createdAt)

data class LiveChatMessageResponse(val id: String, val roomId: Long, val senderId: Long, val body: String, val createdAt: Instant)

fun LiveChatMessageResult.toResponse() = LiveChatMessageResponse(id = id, roomId = roomId, senderId = senderId, body = body, createdAt = createdAt)
