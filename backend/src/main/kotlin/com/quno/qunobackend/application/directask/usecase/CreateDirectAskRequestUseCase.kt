package com.quno.qunobackend.application.directask.usecase

import com.quno.qunobackend.application.directask.dto.CreateDirectAskRequestCommand
import com.quno.qunobackend.application.directask.dto.DirectAskRequestResult
import com.quno.qunobackend.domain.common.OutboxEvent
import com.quno.qunobackend.domain.common.OutboxEventRepository
import com.quno.qunobackend.domain.common.OutboxEventTypes
import com.quno.qunobackend.domain.directask.DirectAskNotAcceptedException
import com.quno.qunobackend.domain.directask.DirectAskRequest
import com.quno.qunobackend.domain.directask.DirectAskRequestRepository
import com.quno.qunobackend.domain.directask.DuplicateDirectAskException
import com.quno.qunobackend.domain.directask.SelfDirectAskException
import com.quno.qunobackend.domain.question.QuestionNotFoundException
import com.quno.qunobackend.domain.question.QuestionRepository
import com.quno.qunobackend.domain.user.UserNotFoundException
import com.quno.qunobackend.domain.user.UserRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class CreateDirectAskRequestUseCase(
    private val questionRepository: QuestionRepository,
    private val userRepository: UserRepository,
    private val directAskRequestRepository: DirectAskRequestRepository,
    private val outboxEventRepository: OutboxEventRepository,
) {
    @Transactional
    fun execute(command: CreateDirectAskRequestCommand): DirectAskRequestResult {
        if (command.requesterId == command.targetUserId) throw SelfDirectAskException(command.requesterId)
        questionRepository.findById(command.questionId) ?: throw QuestionNotFoundException(command.questionId)
        val target = userRepository.findById(command.targetUserId) ?: throw UserNotFoundException(command.targetUserId)
        if (!target.acceptsDirectAsk) throw DirectAskNotAcceptedException(command.targetUserId)
        if (directAskRequestRepository.existsPending(command.questionId, command.targetUserId)) {
            throw DuplicateDirectAskException(command.questionId, command.targetUserId)
        }

        val saved = directAskRequestRepository.save(
            DirectAskRequest.request(
                questionId = command.questionId,
                requesterId = command.requesterId,
                targetUserId = command.targetUserId,
                message = command.message,
            ),
        )

        // targetUserId is the only recipient — see DispatchOutboxEventsUseCase's kdoc, same
        // "addressed to one specific person" shape as CONTENT_HIDDEN/MENTIONED_IN_COMMENT.
        outboxEventRepository.save(
            OutboxEvent.create(
                eventType = OutboxEventTypes.DIRECT_ASK_REQUESTED,
                aggregateType = "QUESTION",
                aggregateId = command.questionId,
                payload = """{"directAskRequestId":${saved.id},"actorId":${command.requesterId},"targetUserId":${command.targetUserId}}""",
            ),
        )

        return saved.toResult()
    }
}

internal fun DirectAskRequest.toResult() = DirectAskRequestResult(
    id = requireNotNull(id),
    questionId = questionId,
    requesterId = requesterId,
    targetUserId = targetUserId,
    message = message,
    status = status,
    createdAt = createdAt,
    respondedAt = respondedAt,
)
