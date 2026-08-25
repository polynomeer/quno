package com.quno.qunobackend.domain.reputation

/**
 * A purely activity-based reputation approximation (ADR-0018) — no peer review, no abuse
 * detection. Accepted answers and Super Answer designations are weighted heavily since they're
 * the strongest signal of an actually-useful contribution, closer to what "전문가" should mean
 * than raw activity counts.
 */
data class UserReputation(
    val userId: Long,
    val questionCount: Long,
    val answerCount: Long,
    val acceptedAnswerCount: Long,
    val superAnswerCount: Long,
) {
    val score: Long
        get() = questionCount * 1 + answerCount * 2 + acceptedAnswerCount * 15 + superAnswerCount * 10
}
