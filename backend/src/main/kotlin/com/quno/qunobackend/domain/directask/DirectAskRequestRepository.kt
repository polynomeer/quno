package com.quno.qunobackend.domain.directask

/** Port implemented by infrastructure/persistence/jpa/adapter/DirectAskRequestRepositoryAdapter. */
interface DirectAskRequestRepository {
    fun save(request: DirectAskRequest): DirectAskRequest
    fun findById(id: Long): DirectAskRequest?
    /** True if an AWAITING_PAYMENT or PENDING request already exists for this pair — see V21's
     * partial unique index. */
    fun existsOpen(questionId: Long, targetUserId: Long): Boolean

    /** Most recent first. */
    fun findAllByRequesterId(requesterId: Long): List<DirectAskRequest>

    /** Most recent first. */
    fun findAllByTargetUserId(targetUserId: Long): List<DirectAskRequest>
}
