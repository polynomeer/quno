package com.quno.qunobackend.domain.snapshot

import java.security.MessageDigest
import java.time.Instant

/** Who produced a snapshot (ADR-0083) — shown as provenance, not proof: an open client can't hold a secret key. */
enum class SnapshotSource { COLLECTOR, MANUAL }

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
