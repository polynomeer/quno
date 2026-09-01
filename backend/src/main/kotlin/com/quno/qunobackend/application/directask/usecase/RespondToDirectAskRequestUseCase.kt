package com.quno.qunobackend.application.directask.usecase

import com.quno.qunobackend.domain.common.OutboxEvent
import com.quno.qunobackend.domain.common.OutboxEventRepository
import com.quno.qunobackend.domain.common.OutboxEventTypes
import com.quno.qunobackend.domain.directask.DirectAskAccessDeniedException
import com.quno.qunobackend.domain.directask.DirectAskPaymentRepository
import com.quno.qunobackend.domain.directask.DirectAskRequestAlreadyRespondedException
import com.quno.qunobackend.domain.directask.DirectAskRequestNotFoundException
import com.quno.qunobackend.domain.directask.DirectAskRequestRepository
import com.quno.qunobackend.domain.directask.DirectAskRequestStatus
import com.quno.qunobackend.domain.directask.PaymentGateway
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/** Declining automatically refunds the fee (Phase 25, ADR-0037) — it wouldn't be fair to charge
 * the requester for a request the target never even acted on. Accepting keeps the payment as-is. */
@Service
class RespondToDirectAskRequestUseCase(
    private val directAskRequestRepository: DirectAskRequestRepository,
    private val directAskPaymentRepository: DirectAskPaymentRepository,
    private val paymentGateway: PaymentGateway,
    private val outboxEventRepository: OutboxEventRepository,
) {
    @Transactional
    fun execute(requestId: Long, actorId: Long, accept: Boolean) {
        val request = directAskRequestRepository.findById(requestId) ?: throw DirectAskRequestNotFoundException(requestId)
        if (request.targetUserId != actorId) throw DirectAskAccessDeniedException(requestId)
        if (request.status != DirectAskRequestStatus.PENDING) throw DirectAskRequestAlreadyRespondedException(requestId)

        directAskRequestRepository.save(if (accept) request.accept() else request.decline())

        if (!accept) {
            val payment = directAskPaymentRepository.findByDirectAskRequestId(requestId)
            if (payment != null) {
                paymentGateway.cancel(requireNotNull(payment.tossPaymentKey), "Direct Ask declined")
                directAskPaymentRepository.save(payment.cancel())
            }
        }

        // requesterId is the only recipient — the person who asked wants to know the outcome,
        // not the question's watchers.
        outboxEventRepository.save(
            OutboxEvent.create(
                eventType = if (accept) OutboxEventTypes.DIRECT_ASK_ACCEPTED else OutboxEventTypes.DIRECT_ASK_DECLINED,
                aggregateType = "QUESTION",
                aggregateId = request.questionId,
                payload = """{"directAskRequestId":${request.id},"actorId":$actorId,"requesterId":${request.requesterId}}""",
            ),
        )
    }
}
