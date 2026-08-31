package com.quno.qunobackend.domain.badge

interface BadgeRepository {
    /** Sum of vote values received across the user's own questions and answers (0 if none). */
    fun sumVoteScoreReceived(userId: Long): Long
}
