package com.quno.qunobackend.application.directask.usecase

import com.quno.qunobackend.domain.directask.DirectAskRequest
import com.quno.qunobackend.domain.directask.DirectAskRequestRepository
import com.quno.qunobackend.domain.directask.DirectAskRequestStatus

class InMemoryDirectAskRequestRepository : DirectAskRequestRepository {
    private val requestsById = mutableMapOf<Long, DirectAskRequest>()
    private var nextId = 1L

    override fun save(request: DirectAskRequest): DirectAskRequest {
        val saved = if (request.id == null) {
            DirectAskRequest.reconstitute(
                id = nextId++,
                questionId = request.questionId,
                requesterId = request.requesterId,
                targetUserId = request.targetUserId,
                message = request.message,
                status = request.status,
                createdAt = request.createdAt,
                respondedAt = request.respondedAt,
            )
        } else {
            request
        }
        requestsById[requireNotNull(saved.id)] = saved
        return saved
    }

    override fun findById(id: Long): DirectAskRequest? = requestsById[id]

    override fun existsPending(questionId: Long, targetUserId: Long): Boolean =
        requestsById.values.any {
            it.questionId == questionId && it.targetUserId == targetUserId && it.status == DirectAskRequestStatus.PENDING
        }

    override fun findAllByRequesterId(requesterId: Long): List<DirectAskRequest> =
        requestsById.values.filter { it.requesterId == requesterId }.sortedByDescending { it.createdAt }

    override fun findAllByTargetUserId(targetUserId: Long): List<DirectAskRequest> =
        requestsById.values.filter { it.targetUserId == targetUserId }.sortedByDescending { it.createdAt }
}
