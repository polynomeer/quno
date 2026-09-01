package com.quno.qunobackend.application.directask.usecase

import com.quno.qunobackend.application.directask.dto.ConfirmDirectAskPaymentCommand
import com.quno.qunobackend.application.directask.dto.DirectAskRequestResult
import com.quno.qunobackend.domain.common.OutboxEvent
import com.quno.qunobackend.domain.common.OutboxEventRepository
import com.quno.qunobackend.domain.common.OutboxEventTypes
import com.quno.qunobackend.domain.directask.DirectAskAccessDeniedException
import com.quno.qunobackend.domain.directask.DirectAskPaymentNotFoundException
import com.quno.qunobackend.domain.directask.DirectAskPaymentRepository
import com.quno.qunobackend.domain.directask.DirectAskPaymentStatus
import com.quno.qunobackend.domain.directask.DirectAskRequestNotFoundException
import com.quno.qunobackend.domain.directask.DirectAskRequestRepository
import com.quno.qunobackend.domain.directask.PaymentAlreadyProcessedException
import com.quno.qunobackend.domain.directask.PaymentAmountMismatchException
import com.quno.qunobackend.domain.directask.PaymentGateway
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * Called after the client completes Toss's payment widget and is redirected back with a
 * `paymentKey` (Phase 25, ADR-0037). This is where the request actually becomes visible to its
 * target — `DIRECT_ASK_REQUESTED` fires here, not at creation time.
 */
@Service
class ConfirmDirectAskPaymentUseCase(
    private val directAskPaymentRepository: DirectAskPaymentRepository,
    private val directAskRequestRepository: DirectAskRequestRepository,
    private val paymentGateway: PaymentGateway,
    private val outboxEventRepository: OutboxEventRepository,
) {
    @Transactional
    fun execute(command: ConfirmDirectAskPaymentCommand): DirectAskRequestResult {
        val payment = directAskPaymentRepository.findByOrderId(command.orderId)
            ?: throw DirectAskPaymentNotFoundException(command.orderId)
        val request = directAskRequestRepository.findById(payment.directAskRequestId)
            ?: throw DirectAskRequestNotFoundException(payment.directAskRequestId)
        // Only the requester who opened this payment may confirm it — orderId/paymentKey are
        // unguessable in practice, but the endpoint shouldn't rely on that alone.
        if (request.requesterId != command.actorId) throw DirectAskAccessDeniedException(requireNotNull(request.id))
        if (payment.status != DirectAskPaymentStatus.PENDING) throw PaymentAlreadyProcessedException(command.orderId)
        // Never trust the client's amount for the actual Toss call — compare against what was
        // recorded when the payment was opened first.
        if (payment.amount != command.amount) throw PaymentAmountMismatchException(payment.amount, command.amount)

        val confirmed = paymentGateway.confirm(command.paymentKey, command.orderId, command.amount)
        directAskPaymentRepository.save(payment.confirm(confirmed.paymentKey))
        val activated = directAskRequestRepository.save(request.activate())

        // targetUserId is the only recipient — see DispatchOutboxEventsUseCase's kdoc, same
        // "addressed to one specific person" shape as CONTENT_HIDDEN/MENTIONED_IN_COMMENT.
        outboxEventRepository.save(
            OutboxEvent.create(
                eventType = OutboxEventTypes.DIRECT_ASK_REQUESTED,
                aggregateType = "QUESTION",
                aggregateId = activated.questionId,
                payload = """{"directAskRequestId":${activated.id},"actorId":${activated.requesterId},"targetUserId":${activated.targetUserId}}""",
            ),
        )

        return activated.toResult()
    }
}
