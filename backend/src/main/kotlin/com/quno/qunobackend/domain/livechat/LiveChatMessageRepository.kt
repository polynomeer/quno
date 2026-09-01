package com.quno.qunobackend.domain.livechat

/** Port implemented by infrastructure/persistence/mongo/LiveChatMessageRepositoryAdapter. */
interface LiveChatMessageRepository {
    fun save(message: LiveChatMessage): LiveChatMessage

    /** Most recent [limit] messages for a room, oldest first — ready to render top-to-bottom. */
    fun findRecentByRoomId(roomId: Long, limit: Int): List<LiveChatMessage>
}
