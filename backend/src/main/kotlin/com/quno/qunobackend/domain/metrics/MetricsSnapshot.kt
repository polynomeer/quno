package com.quno.qunobackend.domain.metrics

/**
 * Aggregate counts backing the MVP success indicators from docs/product/mvp-scope.md.
 * Rates are 0.0..1.0 fractions, not percentages.
 *
 * CTR and D1/D7 retention are intentionally not modeled here — they require client-side
 * event tracking that doesn't exist yet (no frontend has been built this phase).
 */
data class MetricsSnapshot(
    val totalQuestions: Long,
    val revisedQuestions: Long,
    val answeredQuestions: Long,
    val resolvedQuestions: Long,
    val watchedQuestions: Long,
    val livingQuestions: Long,
) {
    val revisionRate: Double get() = rate(revisedQuestions, totalQuestions)
    val answerRate: Double get() = rate(answeredQuestions, totalQuestions)
    val acceptRate: Double get() = rate(resolvedQuestions, totalQuestions)
    val wardCoverageRate: Double get() = rate(watchedQuestions, totalQuestions)
    val livingQuestionRate: Double get() = rate(livingQuestions, totalQuestions)

    private fun rate(numerator: Long, denominator: Long): Double =
        if (denominator == 0L) 0.0 else numerator.toDouble() / denominator
}
