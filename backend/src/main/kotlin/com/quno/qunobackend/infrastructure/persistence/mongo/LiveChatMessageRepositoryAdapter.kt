package com.quno.qunobackend.infrastructure.persistence.mongo

import com.quno.qunobackend.domain.livechat.LiveChatMessage
import com.quno.qunobackend.domain.livechat.LiveChatMessageRepository
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Component

@Component
class LiveChatMessageRepositoryAdapter(
    private val mongoRepository: LiveChatMessageMongoRepository,
) : LiveChatMessageRepository {

    override fun save(message: LiveChatMessage): LiveChatMessage {
        val document = LiveChatMessageDocument(
            id = message.id,
            roomId = message.roomId,
            senderId = message.senderId,
            body = message.body,
            createdAt = message.createdAt,
        )
        return mongoRepository.save(document).toDomain()
    }

    override fun findRecentByRoomId(roomId: Long, limit: Int): List<LiveChatMessage> =
        mongoRepository.findByRoomIdOrderByCreatedAtDesc(roomId, PageRequest.of(0, limit))
            .map { it.toDomain() }
            .reversed()

    private fun LiveChatMessageDocument.toDomain(): LiveChatMessage =
        LiveChatMessage.reconstitute(id = requireNotNull(id), roomId = roomId, senderId = senderId, body = body, createdAt = createdAt)
}
