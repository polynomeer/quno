package com.quno.qunobackend.domain.flow

/**
 * Signals derived from existing event/timestamp data with no new event log of their own —
 * shared by the advanced Dashboard (PLAN.md 10.2) and Quno Flow (PLAN.md 10.3).
 */
interface FlowRepository {
    /** Questions marked OUTDATED that were later revised — "reopened", most recent revision first. */
    fun findRecentlyReopenedQuestionIds(limit: Int): List<Long>

    /** Clusters whose Super Answer was (re)designated most recently. */
    fun findRecentlySuperAnsweredClusterIds(limit: Int): List<Long>
}
