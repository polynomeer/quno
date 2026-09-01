package com.quno.qunobackend.infrastructure.persistence.mongo

import org.springframework.data.domain.Pageable
import org.springframework.data.mongodb.repository.MongoRepository

interface LiveChatMessageMongoRepository : MongoRepository<LiveChatMessageDocument, String> {
    fun findByRoomIdOrderByCreatedAtDesc(roomId: Long, pageable: Pageable): List<LiveChatMessageDocument>
}
