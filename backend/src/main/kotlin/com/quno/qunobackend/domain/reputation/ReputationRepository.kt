package com.quno.qunobackend.domain.reputation

interface ReputationRepository {
    /** Returns zeroed counts for a user with no activity — never null. */
    fun compute(userId: Long): UserReputation
}
