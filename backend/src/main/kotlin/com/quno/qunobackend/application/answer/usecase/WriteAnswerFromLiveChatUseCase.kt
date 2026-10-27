package com.quno.qunobackend.application.answer.usecase

import com.quno.qunobackend.application.answer.dto.AnswerResult
import com.quno.qunobackend.application.answer.dto.WriteAnswerCommand
import com.quno.qunobackend.application.livechat.LiveChatParticipantPolicy
import com.quno.qunobackend.domain.livechat.LiveChatRoomQuestionMismatchException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * Publishes an answer that cites the live chat it was summarised from (Phase 71, ADR-0073). Only
 * adds the room checks — the write itself (versions, NEW_ANSWER outbox, acceptance) is the plain
 * [WriteAnswerUseCase], so a chat-sourced answer behaves exactly like any other answer.
 */
@Service
class WriteAnswerFromLiveChatUseCase(
    private val liveChatParticipantPolicy: LiveChatParticipantPolicy,
    private val writeAnswerUseCase: WriteAnswerUseCase,
) {
    @Transactional
    fun execute(command: WriteAnswerCommand, liveChatRoomId: Long): AnswerResult {
        val room = liveChatParticipantPolicy.requireParticipant(liveChatRoomId, command.authorId)
        if (room.questionId != command.questionId) {
            throw LiveChatRoomQuestionMismatchException(liveChatRoomId, command.questionId)
        }
        return writeAnswerUseCase.execute(command.copy(sourceLiveChatRoomId = liveChatRoomId))
    }
}
