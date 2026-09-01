package com.quno.qunobackend.application.directask.usecase

import com.quno.qunobackend.application.directask.dto.CreateDirectAskRequestCommand
import com.quno.qunobackend.application.directask.dto.CreateDirectAskRequestResult
import com.quno.qunobackend.application.directask.dto.DirectAskPaymentResult
import com.quno.qunobackend.application.directask.dto.DirectAskRequestResult
import com.quno.qunobackend.domain.directask.DirectAskNotAcceptedException
import com.quno.qunobackend.domain.directask.DirectAskPayment
import com.quno.qunobackend.domain.directask.DirectAskPaymentRepository
import com.quno.qunobackend.domain.directask.DirectAskRequest
import com.quno.qunobackend.domain.directask.DirectAskRequestRepository
import com.quno.qunobackend.domain.directask.DuplicateDirectAskException
import com.quno.qunobackend.domain.directask.SelfDirectAskException
import com.quno.qunobackend.domain.question.QuestionNotFoundException
import com.quno.qunobackend.domain.question.QuestionRepository
import com.quno.qunobackend.domain.user.UserNotFoundException
import com.quno.qunobackend.domain.user.UserRepository
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

/**
 * Creates the request AND opens its payment in the same transaction (Phase 25, ADR-0037) — the
 * request starts AWAITING_PAYMENT and is invisible to the target (no DIRECT_ASK_REQUESTED event
 * yet) until [ConfirmDirectAskPaymentUseCase] confirms the charge actually went through.
 */
@Service
class CreateDirectAskRequestUseCase(
    private val questionRepository: QuestionRepository,
    private val userRepository: UserRepository,
    private val directAskRequestRepository: DirectAskRequestRepository,
    private val directAskPaymentRepository: DirectAskPaymentRepository,
    @Value("\${quno.direct-ask.fee-amount}") private val feeAmount: Long,
    @Value("\${quno.toss.client-key}") private val tossClientKey: String,
) {
    @Transactional
    fun execute(command: CreateDirectAskRequestCommand): CreateDirectAskRequestResult {
        if (command.requesterId == command.targetUserId) throw SelfDirectAskException(command.requesterId)
        questionRepository.findById(command.questionId) ?: throw QuestionNotFoundException(command.questionId)
        val target = userRepository.findById(command.targetUserId) ?: throw UserNotFoundException(command.targetUserId)
        if (!target.acceptsDirectAsk) throw DirectAskNotAcceptedException(command.targetUserId)
        if (directAskRequestRepository.existsOpen(command.questionId, command.targetUserId)) {
            throw DuplicateDirectAskException(command.questionId, command.targetUserId)
        }

        val savedRequest = directAskRequestRepository.save(
            DirectAskRequest.request(
                questionId = command.questionId,
                requesterId = command.requesterId,
                targetUserId = command.targetUserId,
                message = command.message,
            ),
        )
        val savedPayment = directAskPaymentRepository.save(
            DirectAskPayment.open(
                directAskRequestId = requireNotNull(savedRequest.id),
                orderId = UUID.randomUUID().toString(),
                amount = feeAmount,
            ),
        )

        return CreateDirectAskRequestResult(
            request = savedRequest.toResult(),
            payment = DirectAskPaymentResult(
                orderId = savedPayment.orderId,
                amount = savedPayment.amount,
                status = savedPayment.status,
                clientKey = tossClientKey,
            ),
        )
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
