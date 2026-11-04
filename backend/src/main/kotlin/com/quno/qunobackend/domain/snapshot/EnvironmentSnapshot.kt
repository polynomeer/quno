package com.quno.qunobackend.domain.snapshot

import java.security.MessageDigest
import java.time.Instant

/** Who produced a snapshot (ADR-0083) — shown as provenance, not proof: an open client can't hold a secret key. */
enum class SnapshotSource { COLLECTOR, MANUAL }

/**
 * What a failure looked like (Phase 93.3, ADR-0084), named by the collector from the failing
 * command's output. The server keeps it as a column so snapshots can be grouped by symptom and
 * environment; a name it doesn't know is stored as null rather than refused.
 */
enum class SnapshotSymptom { OUT_OF_MEMORY, DB_TIMEOUT, DEPENDENCY_RESOLUTION, TEST_FAILURE, BUILD_FAILURE }

class InvalidEnvironmentSnapshotException(message: String) : RuntimeException(message)

/**
 * A question's environment snapshot (Phase 93, ADR-0070/ADR-0083): the scrubbed `quno.snapshot`
 * document plus what the server derived from it — the fingerprint it computed itself and how many
 * secret-looking values it still had to redact. One per question, the latest one.
 */
data class EnvironmentSnapshot(
    val questionId: Long,
    val versionNumber: Int,
    val schemaVersion: Int,
    val source: SnapshotSource,
    val collector: String?,
    val fingerprint: String,
    val serverRedactions: Int,
    /** From `tier1.symptom`; null for Tier 0-only snapshots. */
    val symptom: SnapshotSymptom?,
    /** The scrubbed JSON document as stored. */
    val document: String,
    val createdAt: Instant,
)

/** What the server reads out of a submitted document before storing it. */
data class NormalizedSnapshot(
    val schemaVersion: Int,
    val source: SnapshotSource,
    val collector: String?,
    val fingerprint: String,
    val serverRedactions: Int,
    val symptom: SnapshotSymptom?,
    val document: String,
)

/** Port implemented by infrastructure/snapshot/JacksonSnapshotNormalizer — JSON handling stays out of the domain. */
interface SnapshotNormalizer {
    /** Validates, re-scrubs every string value and fingerprints [rawJson]; throws [InvalidEnvironmentSnapshotException]. */
    fun normalize(rawJson: String): NormalizedSnapshot
}

interface EnvironmentSnapshotRepository {
    fun replaceForQuestion(snapshot: EnvironmentSnapshot)

    fun findByQuestionId(questionId: Long): EnvironmentSnapshot?

    /** Distinct environments by how many public questions share them (Phase 93.6, ADR-0089). */
    fun findEnvironmentGroups(limit: Int): List<EnvironmentGroup> = emptyList()
}

/** The fields of a snapshot that decide its fingerprint, read back out of the document. */
data class EnvironmentCore(
    val osName: String?,
    val arch: String?,
    val docker: Boolean,
    val wsl: Boolean,
    val runtimes: List<FingerprintRuntime>,
) {
    /** `macOS arm64 · java 21 · node 22`: patch versions stay out, like the fingerprint. */
    val label: String
        get() = buildList {
            add(listOfNotNull(osName, arch).joinToString(" ").ifEmpty { "알 수 없는 OS" })
            if (docker) add("Docker")
            if (wsl) add("WSL")
            runtimes.sortedBy { it.name.lowercase() }.forEach { add("${it.name} ${EnvironmentFingerprint.major(it.version)}") }
        }.joinToString(" · ")
}

/** One distinct environment: the fingerprint is the dedup key, the count is what impact views read. */
data class EnvironmentGroup(val fingerprint: String, val questionCount: Long, val lastSeenAt: Instant, val core: EnvironmentCore)

/** Reads [EnvironmentCore] out of a stored document — JSON handling stays out of the domain. */
fun interface EnvironmentCoreReader {
    fun read(document: String): EnvironmentCore
}

/**
 * What to put in the reusable workflow to come close to a reported environment (Phase 93.6,
 * ADR-0089). GitHub's hosted runners are the only choice at this stage, so this is the nearest
 * image, not the same machine; [notes] says what could not be matched.
 */
data class EnvironmentRecipe(val runsOn: String, val javaVersion: String?, val nodeVersion: String?, val notes: List<String>) {
    companion object {
        fun from(core: EnvironmentCore): EnvironmentRecipe {
            val os = core.osName.orEmpty().lowercase()
            val arm = core.arch.orEmpty().lowercase().let { it.contains("arm") || it.contains("aarch") }
            val runsOn = when {
                os.contains("mac") || os.contains("darwin") -> if (arm) "macos-latest" else "macos-13"
                os.contains("windows") -> "windows-latest"
                arm -> "ubuntu-24.04-arm"
                else -> "ubuntu-latest"
            }
            val java = core.runtimes.firstOrNull { it.name.equals("java", ignoreCase = true) }
            val node = core.runtimes.firstOrNull { it.name.equals("node", ignoreCase = true) }
            val notes = buildList {
                if (core.osName.isNullOrBlank()) add("OS를 알 수 없어 Linux 러너로 잡았어요.")
                if (core.docker) add("보고된 환경은 Docker 안이었어요. Dockerfile 캡슐이 더 가까워요.")
                if (core.wsl) add("보고된 환경은 WSL이었어요. Linux 러너로 잡았어요.")
                if (java?.vendor != null && !java.vendor.contains("adoptium", ignoreCase = true) && !java.vendor.contains("temurin", ignoreCase = true)) {
                    add("보고된 JDK는 ${java.vendor}지만 워크플로는 Temurin을 써요.")
                }
            }
            return EnvironmentRecipe(
                runsOn = runsOn,
                javaVersion = java?.let { EnvironmentFingerprint.major(it.version) }?.takeIf { it != "?" },
                nodeVersion = node?.let { EnvironmentFingerprint.major(it.version) }?.takeIf { it != "?" },
                notes = notes,
            )
        }
    }
}

/** A runtime as the fingerprint sees it: name, vendor, major version. */
data class FingerprintRuntime(val name: String, val vendor: String?, val version: String?)

/**
 * The environment fingerprint (ADR-0083): SHA-256 of a normalised tuple — OS name, architecture,
 * container flags, and each runtime's name, vendor and **major** version — first 16 hex digits.
 * Patch versions stay out so that the same environment groups together, the unit the impact
 * matrix (Phase 72) counts in. Always computed here, never taken from the client.
 */
object EnvironmentFingerprint {
    const val SUPPORTED_SCHEMA_VERSION = 1
    const val MAX_DOCUMENT_BYTES = 64 * 1024
    const val MAX_DEPENDENCIES = 500

    /** 0 = always, 1 = diagnostics of a failure, 2 = opt-in runtime state (ADR-0070). */
    val KNOWN_TIERS = setOf(0, 1, 2)

    fun of(osName: String?, arch: String?, docker: Boolean, wsl: Boolean, runtimes: List<FingerprintRuntime>): String {
        val tuple = buildList {
            add("os=${osName.norm()}")
            add("arch=${arch.norm()}")
            add("docker=$docker")
            add("wsl=$wsl")
            runtimes.sortedBy { it.name.lowercase() }.forEach { add("rt=${it.name.norm()}|${it.vendor.norm()}|${major(it.version)}") }
        }.joinToString("\n")
        return MessageDigest.getInstance("SHA-256").digest(tuple.toByteArray()).joinToString("") { "%02x".format(it) }.take(16)
    }

    /** `21.0.5` → `21`, `1.8.0_412` → `8` (Java's old numbering), `v22.11.0` → `22`. */
    fun major(version: String?): String {
        val numbers = Regex("""\d+""").findAll(version.orEmpty()).map { it.value }.toList()
        val first = numbers.firstOrNull() ?: return "?"
        return if (first == "1" && numbers.size > 1) numbers[1] else first
    }

    private fun String?.norm(): String = this?.trim()?.lowercase().orEmpty().ifEmpty { "?" }
}
