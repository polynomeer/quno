package com.quno.qunobackend.application.directask.usecase

import com.quno.qunobackend.application.directask.dto.DirectAskRequestResult
import com.quno.qunobackend.domain.directask.DirectAskRequestRepository
import com.quno.qunobackend.domain.directask.DirectAskRequestStatus
import org.springframework.stereotype.Service

@Service
class ListMyDirectAsksUseCase(
    private val directAskRequestRepository: DirectAskRequestRepository,
) {
    /** Includes AWAITING_PAYMENT — the requester should be able to see (and resume) a request
     * they haven't finished paying for. */
    fun executeSent(userId: Long): List<DirectAskRequestResult> =
        directAskRequestRepository.findAllByRequesterId(userId).map { it.toResult() }

    /** Excludes AWAITING_PAYMENT (Phase 25, ADR-0037) — the target only ever sees a request once
     * payment is confirmed, same invariant [ConfirmDirectAskPaymentUseCase] enforces for
     * notifications. */
    fun executeReceived(userId: Long): List<DirectAskRequestResult> =
        directAskRequestRepository.findAllByTargetUserId(userId)
            .filter { it.status != DirectAskRequestStatus.AWAITING_PAYMENT }
            .map { it.toResult() }
}
