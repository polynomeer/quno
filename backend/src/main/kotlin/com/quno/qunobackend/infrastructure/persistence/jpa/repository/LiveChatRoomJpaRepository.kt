package com.quno.qunobackend.infrastructure.persistence.jpa.repository

import com.quno.qunobackend.infrastructure.persistence.jpa.entity.LiveChatRoomJpaEntity
import org.springframework.data.jpa.repository.JpaRepository

interface LiveChatRoomJpaRepository : JpaRepository<LiveChatRoomJpaEntity, Long> {
    fun findByQuestionId(questionId: Long): LiveChatRoomJpaEntity?
}
