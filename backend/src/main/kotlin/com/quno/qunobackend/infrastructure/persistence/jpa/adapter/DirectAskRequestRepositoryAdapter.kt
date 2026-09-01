package com.quno.qunobackend.infrastructure.persistence.jpa.adapter

import com.quno.qunobackend.domain.directask.DirectAskRequest
import com.quno.qunobackend.domain.directask.DirectAskRequestRepository
import com.quno.qunobackend.domain.directask.DirectAskRequestStatus
import com.quno.qunobackend.infrastructure.persistence.jpa.entity.DirectAskRequestJpaEntity
import com.quno.qunobackend.infrastructure.persistence.jpa.repository.DirectAskRequestJpaRepository
import org.springframework.stereotype.Component

@Component
class DirectAskRequestRepositoryAdapter(
    private val jpaRepository: DirectAskRequestJpaRepository,
) : DirectAskRequestRepository {

    override fun save(request: DirectAskRequest): DirectAskRequest {
        val entity = DirectAskRequestJpaEntity(
            id = request.id,
            questionId = request.questionId,
            requesterId = request.requesterId,
            targetUserId = request.targetUserId,
            message = request.message,
            status = request.status,
            createdAt = request.createdAt,
            respondedAt = request.respondedAt,
        )
        return jpaRepository.save(entity).toDomain()
    }

    override fun findById(id: Long): DirectAskRequest? = jpaRepository.findById(id).orElse(null)?.toDomain()

    override fun existsOpen(questionId: Long, targetUserId: Long): Boolean =
        jpaRepository.existsByQuestionIdAndTargetUserIdAndStatusIn(
            questionId, targetUserId, listOf(DirectAskRequestStatus.AWAITING_PAYMENT, DirectAskRequestStatus.PENDING),
        )

    override fun findAllByRequesterId(requesterId: Long): List<DirectAskRequest> =
        jpaRepository.findAllByRequesterIdOrderByCreatedAtDesc(requesterId).map { it.toDomain() }

    override fun findAllByTargetUserId(targetUserId: Long): List<DirectAskRequest> =
        jpaRepository.findAllByTargetUserIdOrderByCreatedAtDesc(targetUserId).map { it.toDomain() }

    private fun DirectAskRequestJpaEntity.toDomain(): DirectAskRequest = DirectAskRequest.reconstitute(
        id = requireNotNull(id),
        questionId = questionId,
        requesterId = requesterId,
        targetUserId = targetUserId,
        message = message,
        status = status,
        createdAt = createdAt,
        respondedAt = respondedAt,
    )
}
