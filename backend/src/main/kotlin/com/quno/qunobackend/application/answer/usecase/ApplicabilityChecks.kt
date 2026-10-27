package com.quno.qunobackend.application.answer.usecase

import com.quno.qunobackend.domain.stack.ApplicabilityRange
import com.quno.qunobackend.domain.stack.InvalidApplicabilityRangeException

/** Shared by write and revise: one range per package, at most [ApplicabilityRange.MAX_PER_ANSWER] (ADR-0077). */
internal fun checkedApplicability(ranges: List<ApplicabilityRange>): List<ApplicabilityRange> {
    val duplicate = ranges.groupBy { it.ecosystem to it.name }.values.firstOrNull { it.size > 1 }
    if (duplicate != null) throw InvalidApplicabilityRangeException("${duplicate.first().name}: give one range per package")
    if (ranges.size > ApplicabilityRange.MAX_PER_ANSWER) {
        throw InvalidApplicabilityRangeException("an answer can carry at most ${ApplicabilityRange.MAX_PER_ANSWER} ranges")
    }
    return ranges
}
