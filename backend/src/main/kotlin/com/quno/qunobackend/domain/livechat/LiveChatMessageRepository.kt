package com.quno.qunobackend.domain.livechat

/** Port implemented by infrastructure/persistence/mongo/LiveChatMessageRepositoryAdapter. */
interface LiveChatMessageRepository {
    fun save(message: LiveChatMessage): LiveChatMessage

    /** Most recent [limit] messages for a room, oldest first — ready to render top-to-bottom. */
    fun findRecentByRoomId(roomId: Long, limit: Int): List<LiveChatMessage>

    /** The messages among [ids] that belong to [roomId]; ids from other rooms are simply absent. */
    fun findByRoomIdAndIds(roomId: Long, ids: Collection<String>): List<LiveChatMessage>

    fun existsByRoomIdAndSenderId(roomId: Long, senderId: Long): Boolean
}
