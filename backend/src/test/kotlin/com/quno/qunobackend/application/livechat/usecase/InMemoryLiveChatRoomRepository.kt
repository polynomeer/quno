package com.quno.qunobackend.application.livechat.usecase

import com.quno.qunobackend.domain.livechat.LiveChatRoom
import com.quno.qunobackend.domain.livechat.LiveChatRoomRepository

class InMemoryLiveChatRoomRepository : LiveChatRoomRepository {
    private val roomsById = mutableMapOf<Long, LiveChatRoom>()
    private var nextId = 1L

    override fun save(room: LiveChatRoom): LiveChatRoom {
        val saved = if (room.id == null) {
            LiveChatRoom.reconstitute(id = nextId++, questionId = room.questionId, createdBy = room.createdBy, createdAt = room.createdAt)
        } else {
            room
        }
        roomsById[requireNotNull(saved.id)] = saved
        return saved
    }

    override fun findById(id: Long): LiveChatRoom? = roomsById[id]

    override fun findByQuestionId(questionId: Long): LiveChatRoom? = roomsById.values.find { it.questionId == questionId }
}
