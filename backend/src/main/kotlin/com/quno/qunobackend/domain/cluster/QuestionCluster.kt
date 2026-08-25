package com.quno.qunobackend.domain.cluster

import java.time.Instant

/**
 * A user-marked "same problem" grouping of Questions (see
 * docs/architecture/decisions/0016-manual-duplicate-marking-cluster.md) — not the product of
 * automatic similarity analysis. [representativeAnswerId] is the cluster's "Super Answer"
 * (vision.md), designated explicitly (PLAN.md 6.3), not auto-selected.
 */
class QuestionCluster private constructor(
    val id: Long?,
    val representativeAnswerId: Long?,
    val createdAt: Instant,
) {
    fun designateSuperAnswer(answerId: Long): QuestionCluster =
        QuestionCluster(id, answerId, createdAt)

    companion object {
        fun create(): QuestionCluster = QuestionCluster(id = null, representativeAnswerId = null, createdAt = Instant.now())

        fun reconstitute(id: Long, representativeAnswerId: Long?, createdAt: Instant): QuestionCluster =
            QuestionCluster(id, representativeAnswerId, createdAt)
    }
}
