package com.quno.qunobackend.application.livechat

import com.quno.qunobackend.domain.livechat.LiveChatMessageRepository
import com.quno.qunobackend.domain.livechat.LiveChatParticipantRequiredException
import com.quno.qunobackend.domain.livechat.LiveChatRoom
import com.quno.qunobackend.domain.livechat.LiveChatRoomNotFoundException
import com.quno.qunobackend.domain.livechat.LiveChatRoomRepository
import org.springframework.stereotype.Component

/**
 * Who may turn a live chat into an answer (Phase 71, ADR-0073): the person who opened the room,
 * or anyone who has posted in it. Shared by the draft and the publish paths so both enforce the
 * same rule.
 */
@Component
class LiveChatParticipantPolicy(
    private val liveChatRoomRepository: LiveChatRoomRepository,
    private val liveChatMessageRepository: LiveChatMessageRepository,
) {
    fun requireParticipant(roomId: Long, userId: Long): LiveChatRoom {
        val room = liveChatRoomRepository.findById(roomId) ?: throw LiveChatRoomNotFoundException(roomId)
        val participates = room.createdBy == userId || liveChatMessageRepository.existsByRoomIdAndSenderId(roomId, userId)
        if (!participates) throw LiveChatParticipantRequiredException(roomId, userId)
        return room
    }
}
