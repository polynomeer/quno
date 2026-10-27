package com.quno.qunobackend.application.livechat.usecase

import com.quno.qunobackend.application.common.NicknameResolver
import com.quno.qunobackend.application.livechat.LiveChatParticipantPolicy
import com.quno.qunobackend.application.livechat.dto.LiveChatAnswerDraftResult
import com.quno.qunobackend.domain.livechat.InvalidLiveChatMessageSelectionException
import com.quno.qunobackend.domain.livechat.LiveChatMessageRepository
import org.springframework.stereotype.Service

/**
 * Builds a markdown answer draft from the messages a participant picked (Phase 71, ADR-0073).
 * Nothing is saved — the user edits the draft and publishes it through the normal answer write,
 * citing the room. Messages are ordered by time regardless of selection order, and each keeps its
 * sender's nickname so the conversation's contributors stay visible in the answer body.
 */
@Service
class CreateAnswerDraftFromLiveChatUseCase(
    private val liveChatParticipantPolicy: LiveChatParticipantPolicy,
    private val liveChatMessageRepository: LiveChatMessageRepository,
    private val nicknameResolver: NicknameResolver,
) {
    fun execute(roomId: Long, userId: Long, messageIds: List<String>): LiveChatAnswerDraftResult {
        val room = liveChatParticipantPolicy.requireParticipant(roomId, userId)
        val ids = messageIds.distinct()
        if (ids.isEmpty() || ids.size > MAX_MESSAGES) {
            throw InvalidLiveChatMessageSelectionException("Select between 1 and $MAX_MESSAGES messages")
        }
        val messages = liveChatMessageRepository.findByRoomIdAndIds(roomId, ids).sortedBy { it.createdAt }
        if (messages.size != ids.size) {
            throw InvalidLiveChatMessageSelectionException("Some messages are not in live chat room $roomId")
        }

        val nicknames = nicknameResolver.nicknamesOf(messages.map { it.senderId })
        val quoted = messages.joinToString("\n\n") { message ->
            val name = nicknames[message.senderId] ?: "사용자 #${message.senderId}"
            // Two trailing spaces force a markdown line break, so a multi-line chat message keeps
            // its lines inside the quote instead of collapsing into one paragraph.
            val lines = message.body.lines().joinToString("  \n") { "> $it" }
            "**$name**\n$lines"
        }
        val body = "$HEADER\n\n$quoted\n\n## 정리\n\n"

        return LiveChatAnswerDraftResult(roomId = roomId, questionId = room.questionId, body = body, messageCount = messages.size)
    }

    companion object {
        const val MAX_MESSAGES = 50
        private const val HEADER = "실시간 질문방 대화에서 정리한 답변입니다."
    }
}
