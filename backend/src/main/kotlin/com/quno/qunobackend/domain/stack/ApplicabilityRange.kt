package com.quno.qunobackend.domain.stack

class InvalidApplicabilityRangeException(message: String) : RuntimeException(message)

/**
 * Where an answer holds for one package: `[fromVersion, untilVersion)` — from inclusive, until
 * exclusive (Phase 63.3, ADR-0077). An exclusive upper bound names where compatibility breaks
 * ("below 4.0") instead of the last version tried, so the next patch release doesn't fall out of it.
 */
data class ApplicabilityRange(
    val ecosystem: Ecosystem,
    val name: String,
    val fromVersion: String?,
    val untilVersion: String?,
) {
    private val from: Version? = fromVersion?.let { parsed(it) }
    private val until: Version? = untilVersion?.let { parsed(it) }

    init {
        if (name.isBlank() || name.length > StackDependency.MAX_NAME_LENGTH) {
            throw InvalidApplicabilityRangeException("package name must be 1-${StackDependency.MAX_NAME_LENGTH} characters")
        }
        if (from == null && until == null) throw InvalidApplicabilityRangeException("$name: give at least one of fromVersion and untilVersion")
        if (from != null && until != null && from >= until) {
            throw InvalidApplicabilityRangeException("$name: fromVersion $fromVersion must be lower than untilVersion $untilVersion")
        }
    }

    operator fun contains(version: Version): Boolean = (from == null || version >= from) && (until == null || version < until)

    companion object {
        const val MAX_PER_ANSWER = 20

        private fun parsed(raw: String): Version {
            if (raw.length > StackDependency.MAX_VERSION_LENGTH) throw InvalidApplicabilityRangeException("version must be at most ${StackDependency.MAX_VERSION_LENGTH} characters")
            return Version.parseOrNull(raw) ?: throw InvalidApplicabilityRangeException("'$raw' is not a version such as 3.2.0")
        }
    }
}

/** Matches the coordinates that carry one tracked product's version (ADR-0077). [name] ending in `*` matches by prefix. */
data class PackagePattern(val ecosystem: Ecosystem, val name: String) {
    fun matches(ecosystem: Ecosystem, name: String): Boolean = ecosystem == this.ecosystem &&
        if (this.name.endsWith("*")) name.startsWith(this.name.dropLast(1)) else name == this.name
}

enum class ApplicabilityVerdict {
    /** No answer gave a range for this product — fall back to the tag heuristic. */
    NO_RANGE,

    /** Some answer's range covers the version — nothing to revisit. */
    IN_RANGE,

    /** Every range for this product excludes the version. */
    OUT_OF_RANGE,

    ;

    companion object {
        /** How a question's answers' [ranges] stand against a newly released [version] of the product [patterns] describe. */
        fun of(ranges: List<ApplicabilityRange>, patterns: List<PackagePattern>, version: String): ApplicabilityVerdict {
            val relevant = ranges.filter { range -> patterns.any { it.matches(range.ecosystem, range.name) } }
            val released = Version.parseOrNull(version)
            if (relevant.isEmpty() || released == null) return NO_RANGE
            return if (relevant.any { released in it }) IN_RANGE else OUT_OF_RANGE
        }
    }
}

/** Port implemented by infrastructure/persistence/jpa/adapter/AnswerApplicabilityRepositoryAdapter. */
interface AnswerApplicabilityRepository {
    /** Drops whatever the answer had and stores [ranges] for [versionNumber] — latest version only (ADR-0077). */
    fun replaceForAnswer(answerId: Long, questionId: Long, versionNumber: Int, ranges: List<ApplicabilityRange>)

    fun findByAnswerIds(answerIds: Collection<Long>): Map<Long, List<ApplicabilityRange>>

    /** Every range on the questions' answers, grouped by question — what impact detection judges against. */
    fun findByQuestionIds(questionIds: Collection<Long>): Map<Long, List<ApplicabilityRange>>
}
