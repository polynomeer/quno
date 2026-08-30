package com.quno.qunobackend.domain.vote

/** Port for the votes relation table. Score is never persisted — always `sumScore`. */
interface VoteRepository {
    /** Upsert — replaces any existing vote by the same voter on the same target. */
    fun save(vote: Vote): Vote

    /** Idempotent. */
    fun delete(voterId: Long, targetType: VoteTargetType, targetId: Long)

    fun findByVoterAndTarget(voterId: Long, targetType: VoteTargetType, targetId: Long): Vote?

    /** Used by `GET /me/votes` so the frontend can derive "did I vote on X" without N+1 calls. */
    fun findByVoter(voterId: Long): List<Vote>

    fun sumScore(targetType: VoteTargetType, targetId: Long): Long
}
