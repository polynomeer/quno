package com.quno.qunobackend.domain.cluster

import java.time.Instant

/**
 * A user-marked "same problem" grouping of Questions (see
 * docs/architecture/decisions/0016-manual-duplicate-marking-cluster.md) — not the product of
 * automatic similarity analysis. [representativeAnswerId] is the cluster's "Super Answer"
 * (vision.md), designated explicitly (PLAN.md 6.3), not auto-selected. [updatedAt] exists only
 * so "recently got a Super Answer" can be detected for Quno Flow (PLAN.md 10.1) — there is no
 * separate event log for this aggregate.
 */
class QuestionCluster private constructor(
    val id: Long?,
    val representativeAnswerId: Long?,
    val createdAt: Instant,
    val updatedAt: Instant,
) {
    fun designateSuperAnswer(answerId: Long): QuestionCluster =
        QuestionCluster(id, answerId, createdAt, Instant.now())

    companion object {
        fun create(): QuestionCluster {
            val now = Instant.now()
            return QuestionCluster(id = null, representativeAnswerId = null, createdAt = now, updatedAt = now)
        }

        fun reconstitute(id: Long, representativeAnswerId: Long?, createdAt: Instant, updatedAt: Instant): QuestionCluster =
            QuestionCluster(id, representativeAnswerId, createdAt, updatedAt)
    }
}
