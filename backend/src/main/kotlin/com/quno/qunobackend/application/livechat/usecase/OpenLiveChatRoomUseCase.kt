package com.quno.qunobackend.application.livechat.usecase

import com.quno.qunobackend.application.livechat.dto.LiveChatRoomResult
import com.quno.qunobackend.domain.common.OutboxEvent
import com.quno.qunobackend.domain.common.OutboxEventRepository
import com.quno.qunobackend.domain.common.OutboxEventTypes
import com.quno.qunobackend.domain.livechat.LiveChatRoom
import com.quno.qunobackend.domain.livechat.LiveChatRoomRepository
import com.quno.qunobackend.domain.question.QuestionNotFoundException
import com.quno.qunobackend.domain.question.QuestionRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/** Find-or-create — "필요한 경우 즉시 Live Chat을 생성" means opening is idempotent, not a
 * new room every time (Phase 24, ADR-0036). Only a genuinely new room fires a notification. */
@Service
class OpenLiveChatRoomUseCase(
    private val questionRepository: QuestionRepository,
    private val liveChatRoomRepository: LiveChatRoomRepository,
    private val outboxEventRepository: OutboxEventRepository,
) {
    @Transactional
    fun execute(questionId: Long, userId: Long): LiveChatRoomResult {
        val question = questionRepository.findById(questionId) ?: throw QuestionNotFoundException(questionId)
        liveChatRoomRepository.findByQuestionId(questionId)?.let { return it.toResult() }

        val saved = liveChatRoomRepository.save(LiveChatRoom.open(questionId, userId))

        // questionAuthorId: notified even if they never watched their own question — see
        // DispatchOutboxEventsUseCase's kdoc for this recurring pattern.
        outboxEventRepository.save(
            OutboxEvent.create(
                eventType = OutboxEventTypes.LIVE_CHAT_STARTED,
                aggregateType = "QUESTION",
                aggregateId = questionId,
                payload = """{"roomId":${saved.id},"actorId":$userId,"questionAuthorId":${question.authorId}}""",
            ),
        )

        return saved.toResult()
    }
}

internal fun LiveChatRoom.toResult() = LiveChatRoomResult(id = requireNotNull(id), questionId = questionId, createdBy = createdBy, createdAt = createdAt)
