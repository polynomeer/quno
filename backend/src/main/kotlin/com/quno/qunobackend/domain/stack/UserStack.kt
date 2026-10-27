package com.quno.qunobackend.domain.stack

/** Port implemented by infrastructure/persistence/jpa/adapter/UserStackRepositoryAdapter — "내 스택" (Phase 63.5, ADR-0078). */
interface UserStackRepository {
    /** Replaces the user's whole stack; an empty list clears it. */
    fun replaceForUser(userId: Long, dependencies: List<StackDependency>)

    fun findByUserId(userId: Long): List<StackDependency>
}

/** A question that shares [sharedCount] packages (ecosystem + name, any version) with a user's stack. */
data class StackMatch(val questionId: Long, val sharedCount: Int)

/** Read port for Ward suggestions (ADR-0078). */
interface StackMatchRepository {
    /**
     * Non-deleted questions sharing at least one package with [userId]'s stack, leaving out the
     * user's own questions and the ones they already watch. Most shared packages first, then newest.
     */
    fun findQuestionsSharingStack(userId: Long, limit: Int): List<StackMatch>
}
