package com.quno.qunobackend.infrastructure.persistence.jpa.adapter

import com.quno.qunobackend.domain.livechat.LiveChatRoom
import com.quno.qunobackend.domain.livechat.LiveChatRoomRepository
import com.quno.qunobackend.infrastructure.persistence.jpa.entity.LiveChatRoomJpaEntity
import com.quno.qunobackend.infrastructure.persistence.jpa.repository.LiveChatRoomJpaRepository
import org.springframework.stereotype.Component

@Component
class LiveChatRoomRepositoryAdapter(
    private val jpaRepository: LiveChatRoomJpaRepository,
) : LiveChatRoomRepository {

    override fun save(room: LiveChatRoom): LiveChatRoom {
        val entity = LiveChatRoomJpaEntity(id = room.id, questionId = room.questionId, createdBy = room.createdBy, createdAt = room.createdAt)
        return jpaRepository.save(entity).toDomain()
    }

    override fun findById(id: Long): LiveChatRoom? = jpaRepository.findById(id).orElse(null)?.toDomain()

    override fun findByQuestionId(questionId: Long): LiveChatRoom? = jpaRepository.findByQuestionId(questionId)?.toDomain()

    private fun LiveChatRoomJpaEntity.toDomain(): LiveChatRoom =
        LiveChatRoom.reconstitute(id = requireNotNull(id), questionId = questionId, createdBy = createdBy, createdAt = createdAt)
}
