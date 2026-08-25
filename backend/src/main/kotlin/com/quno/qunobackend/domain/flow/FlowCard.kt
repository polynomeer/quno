package com.quno.qunobackend.domain.flow

enum class FlowCardType { POPULAR_QUESTION, TAG_SPIKE, REOPENED_QUESTION, CLUSTER_SUPER_ANSWER }

/**
 * One card in the Quno Flow activity stream (vision.md — "살아 움직이는 Question Network의
 * Activity Stream", see docs/archive/README.md 23장). [headline] is plain, pre-formatted text —
 * there is no per-card timestamp because POPULAR_QUESTION/TAG_SPIKE describe *current* state
 * snapshots with no natural occurrence time, and cards are grouped by type rather than merged
 * into one global timeline (see PLAN.md 10.3).
 */
data class FlowCard(
    val type: FlowCardType,
    val headline: String,
    val questionId: Long? = null,
    val clusterId: Long? = null,
)
