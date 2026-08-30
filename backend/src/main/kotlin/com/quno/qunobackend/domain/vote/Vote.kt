package com.quno.qunobackend.domain.vote

/**
 * Independent side-aggregate, same pattern as Watch — Question/Answer never hold a reference
 * to this (see docs/architecture/decisions/0023-vote-as-side-aggregate-no-reputation-impact.md).
 * Score is never stored on Question/Answer; it's always `SUM(value)` over this table.
 */
data class Vote(
    val voterId: Long,
    val targetType: VoteTargetType,
    val targetId: Long,
    val value: Int,
) {
    companion object {
        fun cast(voterId: Long, targetType: VoteTargetType, targetId: Long, value: Int): Vote {
            if (value != 1 && value != -1) throw InvalidVoteValueException(value)
            return Vote(voterId, targetType, targetId, value)
        }
    }
}
