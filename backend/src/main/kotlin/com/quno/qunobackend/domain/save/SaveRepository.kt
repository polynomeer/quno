package com.quno.qunobackend.domain.save

/**
 * Port for the saves relation table. Pure relation data (hard delete allowed) — Save is a
 * personal bookmark for later reading, not a subscription; see ADR-0025. Deliberately has no
 * "who saved this" query (unlike WatchRepository.findWatcherIds) since nothing needs it.
 */
interface SaveRepository {
    /** Idempotent. */
    fun save(userId: Long, questionId: Long)

    /** Idempotent. */
    fun unsave(userId: Long, questionId: Long)
    fun isSaved(userId: Long, questionId: Long): Boolean
    fun findSavedQuestionIds(userId: Long): List<Long>
}
