package com.quno.qunobackend.domain.badge

enum class BadgeTier { BRONZE, SILVER, GOLD }

/** Raw activity counts a badge's condition is evaluated against — see [BadgeType]. */
data class BadgeStats(
    val questionCount: Long,
    val answerCount: Long,
    val acceptedAnswerCount: Long,
    val superAnswerCount: Long,
    val voteScoreReceived: Long,
)

/**
 * Fixed badge catalog (ADR-0027) — computed live from existing activity aggregates
 * ([BadgeStats], sourced from `ReputationRepository` + one new vote-score query) on every
 * request, never persisted. Thresholds are hardcoded, same trade-off as the Reputation score
 * formula (ADR-0018).
 */
enum class BadgeType(val tier: BadgeTier, private val condition: (BadgeStats) -> Boolean) {
    FIRST_QUESTION(BadgeTier.BRONZE, { it.questionCount >= 1 }),
    FIRST_ANSWER(BadgeTier.BRONZE, { it.answerCount >= 1 }),
    PROBLEM_SOLVER(BadgeTier.SILVER, { it.acceptedAnswerCount >= 5 }),
    WELL_RECEIVED(BadgeTier.SILVER, { it.voteScoreReceived >= 50 }),
    TRUSTED_ANSWERER(BadgeTier.GOLD, { it.acceptedAnswerCount >= 20 }),
    SUPER_ANSWER(BadgeTier.GOLD, { it.superAnswerCount >= 1 }),
    ;

    fun isEarnedBy(stats: BadgeStats): Boolean = condition(stats)

    companion object {
        fun earnedBy(stats: BadgeStats): List<BadgeType> = entries.filter { it.isEarnedBy(stats) }
    }
}
