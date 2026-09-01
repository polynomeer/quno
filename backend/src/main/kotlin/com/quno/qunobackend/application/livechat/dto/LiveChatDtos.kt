package com.quno.qunobackend.application.livechat.dto

import java.time.Instant

data class LiveChatRoomResult(val id: Long, val questionId: Long, val createdBy: Long, val createdAt: Instant)

data class LiveChatMessageResult(val id: String, val roomId: Long, val senderId: Long, val body: String, val createdAt: Instant)
