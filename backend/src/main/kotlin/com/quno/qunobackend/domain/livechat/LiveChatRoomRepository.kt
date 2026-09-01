package com.quno.qunobackend.domain.livechat

/** Port implemented by infrastructure/persistence/jpa/adapter/LiveChatRoomRepositoryAdapter. */
interface LiveChatRoomRepository {
    fun save(room: LiveChatRoom): LiveChatRoom
    fun findById(id: Long): LiveChatRoom?
    fun findByQuestionId(questionId: Long): LiveChatRoom?
}
