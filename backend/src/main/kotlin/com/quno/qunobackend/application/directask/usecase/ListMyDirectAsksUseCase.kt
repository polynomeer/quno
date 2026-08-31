package com.quno.qunobackend.application.directask.usecase

import com.quno.qunobackend.application.directask.dto.DirectAskRequestResult
import com.quno.qunobackend.domain.directask.DirectAskRequestRepository
import org.springframework.stereotype.Service

@Service
class ListMyDirectAsksUseCase(
    private val directAskRequestRepository: DirectAskRequestRepository,
) {
    fun executeSent(userId: Long): List<DirectAskRequestResult> =
        directAskRequestRepository.findAllByRequesterId(userId).map { it.toResult() }

    fun executeReceived(userId: Long): List<DirectAskRequestResult> =
        directAskRequestRepository.findAllByTargetUserId(userId).map { it.toResult() }
}
