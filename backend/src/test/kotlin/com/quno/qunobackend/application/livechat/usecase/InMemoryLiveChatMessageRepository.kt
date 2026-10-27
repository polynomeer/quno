package com.quno.qunobackend.application.livechat.usecase

import com.quno.qunobackend.domain.livechat.LiveChatMessage
import com.quno.qunobackend.domain.livechat.LiveChatMessageRepository

class InMemoryLiveChatMessageRepository : LiveChatMessageRepository {
    private val messages = mutableListOf<LiveChatMessage>()
    private var nextId = 1L

    override fun save(message: LiveChatMessage): LiveChatMessage {
        val saved = if (message.id == null) {
            LiveChatMessage.reconstitute(
                id = (nextId++).toString(),
                roomId = message.roomId,
                senderId = message.senderId,
                body = message.body,
                createdAt = message.createdAt,
            )
        } else {
            message
        }
        messages += saved
        return saved
    }

    override fun findRecentByRoomId(roomId: Long, limit: Int): List<LiveChatMessage> = messages.filter { it.roomId == roomId }.takeLast(limit)

    override fun findByRoomIdAndIds(roomId: Long, ids: Collection<String>): List<LiveChatMessage> = messages.filter { it.roomId == roomId && it.id in ids }

    override fun existsByRoomIdAndSenderId(roomId: Long, senderId: Long): Boolean = messages.any { it.roomId == roomId && it.senderId == senderId }
}
