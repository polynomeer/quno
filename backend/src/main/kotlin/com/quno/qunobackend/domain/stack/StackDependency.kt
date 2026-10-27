package com.quno.qunobackend.domain.stack

/** Where a dependency coordinate lives (Phase 63, ADR-0076). Gradle and Maven share MAVEN coordinates. */
enum class Ecosystem { MAVEN, GRADLE_PLUGIN, NPM }

/**
 * One entry of a question's stack fingerprint: `(ecosystem, name, version)`. [version] is kept as
 * declared (`^3.2.0`, `4.0.8`) and is null for BOM-managed coordinates. Version comparison belongs
 * to answer applicability ranges (Phase 63.3), not here.
 */
data class StackDependency(
    val ecosystem: Ecosystem,
    val name: String,
    val version: String?,
) {
    init {
        require(name.isNotBlank()) { "dependency name must not be blank" }
        require(name.length <= MAX_NAME_LENGTH) { "dependency name must be at most $MAX_NAME_LENGTH characters" }
        require(version == null || version.length <= MAX_VERSION_LENGTH) { "dependency version must be at most $MAX_VERSION_LENGTH characters" }
    }

    companion object {
        const val MAX_NAME_LENGTH = 300
        const val MAX_VERSION_LENGTH = 100

        /** Per question — a real build file rarely declares more, and it bounds what one request writes. */
        const val MAX_PER_QUESTION = 200
    }
}

/** Which file format a parse recognised. */
enum class DependencyFileFormat { GRADLE, MAVEN, NPM }

data class ParsedDependencyFile(
    val format: DependencyFileFormat,
    val dependencies: List<StackDependency>,
)

/** Port implemented by infrastructure/stack/CompositeDependencyFileParser — JSON and XML parsing need libraries the domain stays free of. */
interface DependencyFileParser {
    /**
     * Reads Gradle, Maven or npm dependency declarations. [fileName] is a hint (`pom.xml`,
     * `package.json`, `build.gradle.kts`); without it the format is sniffed from [content].
     * Unreadable content yields an empty list rather than an error, so the preview can say "nothing found".
     */
    fun parse(fileName: String?, content: String): ParsedDependencyFile
}

/** Port implemented by infrastructure/persistence/jpa/adapter/QuestionDependencyRepositoryAdapter. */
interface QuestionDependencyRepository {
    /** Drops whatever the question had and stores [dependencies] for [versionNumber] — latest version only (ADR-0076). */
    fun replaceForQuestion(questionId: Long, versionNumber: Int, dependencies: List<StackDependency>)

    fun findByQuestionId(questionId: Long): List<StackDependency>
}
