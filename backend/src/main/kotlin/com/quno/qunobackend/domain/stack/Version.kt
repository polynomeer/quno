package com.quno.qunobackend.domain.stack

/**
 * A comparable release version for answer applicability ranges (Phase 63.3, ADR-0077). Covers the
 * Maven and npm versions Quno compares: numeric segments compare one by one with missing ones as
 * 0 (`3.2` == `3.2.0`), then the qualifier by stage —
 * `alpha < beta < milestone < rc < snapshot < release < sp` — then the qualifier's trailing number.
 */
class Version private constructor(
    private val numbers: List<Long>,
    private val stage: Int,
    private val qualifier: String,
    private val qualifierNumber: Long,
) : Comparable<Version> {

    override fun compareTo(other: Version): Int {
        for (i in 0 until maxOf(numbers.size, other.numbers.size)) {
            val diff = numbers.getOrElse(i) { 0 }.compareTo(other.numbers.getOrElse(i) { 0 })
            if (diff != 0) return diff
        }
        if (stage != other.stage) return stage.compareTo(other.stage)
        if (qualifier != other.qualifier) return qualifier.compareTo(other.qualifier)
        return qualifierNumber.compareTo(other.qualifierNumber)
    }

    override fun equals(other: Any?): Boolean = other is Version && compareTo(other) == 0

    override fun hashCode(): Int = listOf(numbers.dropLastWhile { it == 0L }, stage, qualifier, qualifierNumber).hashCode()

    companion object {
        private const val UNKNOWN_STAGE = 3
        private const val RELEASE_STAGE = 5

        private val STAGES = mapOf(
            "alpha" to 0, "a" to 0,
            "beta" to 1, "b" to 1,
            "milestone" to 2, "m" to 2,
            "rc" to 3, "cr" to 3,
            "snapshot" to 4,
            "" to RELEASE_STAGE, "ga" to RELEASE_STAGE, "final" to RELEASE_STAGE, "release" to RELEASE_STAGE,
            "sp" to 6,
        )

        private val SHAPE = Regex("""^(\d+(?:\.\d+)*)(?:[.\-]?([a-zA-Z][\w.\-]*))?$""")
        private val QUALIFIER = Regex("""^([a-zA-Z]+)[.\-]?(\d*)""")
        private val RANGE_PREFIX = Regex("""^[\^~>=<\s]+""")

        /** An exact version such as `4.0.8`, `2.2.0-RC1` or `v1.4`; null when it isn't one. */
        fun parseOrNull(raw: String): Version? {
            val text = raw.trim().removePrefix("v").removePrefix("V").substringBefore('+')
            val match = SHAPE.matchEntire(text) ?: return null
            val numbers = match.groupValues[1].split('.').map { it.toLongOrNull() ?: return null }
            val rest = match.groupValues[2].lowercase()
            if (rest.isEmpty()) return Version(numbers, RELEASE_STAGE, "", 0)

            val qualifier = QUALIFIER.find(rest) ?: return null
            val word = qualifier.groupValues[1]
            if (word == "x") return null // `3.x` is a wildcard, not a version
            val stage = STAGES[word]
            return Version(
                numbers = numbers,
                stage = stage ?: UNKNOWN_STAGE,
                // A known word collapses to its stage (`b2` == `beta2`); an unknown one keeps its text for ordering.
                qualifier = if (stage == null) word else "",
                qualifierNumber = qualifier.groupValues[2].toLongOrNull() ?: 0,
            )
        }

        /** A version as declared in a build file — `^3.2.0` and `~1.4` read as their lower bound. */
        fun fromDeclaredOrNull(raw: String): Version? = parseOrNull(raw.replace(RANGE_PREFIX, ""))
    }
}
