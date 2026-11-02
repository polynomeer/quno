package com.quno.qunobackend.infrastructure.snapshot

import com.quno.qunobackend.domain.engine.SecretDetector
import com.quno.qunobackend.domain.snapshot.EnvironmentFingerprint
import com.quno.qunobackend.domain.snapshot.FingerprintRuntime
import com.quno.qunobackend.domain.snapshot.InvalidEnvironmentSnapshotException
import com.quno.qunobackend.domain.snapshot.NormalizedSnapshot
import com.quno.qunobackend.domain.snapshot.SnapshotNormalizer
import com.quno.qunobackend.domain.snapshot.SnapshotSource
import com.quno.qunobackend.domain.snapshot.SnapshotSymptom
import org.springframework.stereotype.Component
import tools.jackson.databind.JsonNode
import tools.jackson.databind.ObjectMapper
import tools.jackson.databind.node.ArrayNode
import tools.jackson.databind.node.ObjectNode
import tools.jackson.databind.node.StringNode

/**
 * Server side of the environment snapshot (ADR-0083): the collector already scrubbed locally, but
 * this is the last line of defence — every string value goes through [SecretDetector] again — and
 * the fingerprint is recomputed here so it can be trusted as a grouping key. Unknown fields are
 * kept (the schema only ever adds); an unknown schemaVersion is refused.
 */
@Component
class JacksonSnapshotNormalizer(
    private val objectMapper: ObjectMapper,
) : SnapshotNormalizer {

    override fun normalize(rawJson: String): NormalizedSnapshot {
        if (rawJson.toByteArray().size > EnvironmentFingerprint.MAX_DOCUMENT_BYTES) {
            throw InvalidEnvironmentSnapshotException("environment snapshot must be at most ${EnvironmentFingerprint.MAX_DOCUMENT_BYTES / 1024}KB")
        }
        val root = runCatching { objectMapper.readTree(rawJson) }.getOrNull() as? ObjectNode
            ?: throw InvalidEnvironmentSnapshotException("environment snapshot must be a JSON object")
        val schemaVersion = root.path("schemaVersion").asInt(0)
        if (schemaVersion != EnvironmentFingerprint.SUPPORTED_SCHEMA_VERSION) {
            throw InvalidEnvironmentSnapshotException("unsupported environment snapshot schemaVersion $schemaVersion")
        }
        val source = runCatching { SnapshotSource.valueOf(root.path("source").asString("MANUAL")) }.getOrElse {
            throw InvalidEnvironmentSnapshotException("source must be COLLECTOR or MANUAL")
        }
        val tier0 = root.path("tier0")
        if ((tier0.path("dependencies").path("items") as? ArrayNode)?.size()?.let { it > EnvironmentFingerprint.MAX_DEPENDENCIES } == true) {
            throw InvalidEnvironmentSnapshotException("an environment snapshot can list at most ${EnvironmentFingerprint.MAX_DEPENDENCIES} dependencies")
        }

        val tiers = (root.path("tiers") as? ArrayNode)?.let { array -> (0 until array.size()).map { array.get(it).asInt(-1) } } ?: listOf(0)
        if (!EnvironmentFingerprint.KNOWN_TIERS.containsAll(tiers)) {
            throw InvalidEnvironmentSnapshotException("tiers must be among ${EnvironmentFingerprint.KNOWN_TIERS}")
        }
        // Tier 2 is opt-in (ADR-0070): runtime state without the opt-in marker is a collector bug, not data to keep.
        if (root.has("tier2") && 2 !in tiers) {
            throw InvalidEnvironmentSnapshotException("tier2 needs 2 in tiers — it is opt-in")
        }
        val symptom = root.path("tier1").path("symptom").textOrNull()?.let { name -> SnapshotSymptom.entries.firstOrNull { it.name == name } }

        val redactions = scrub(root)
        val runtimes = (tier0.path("runtimes") as? ArrayNode)?.let { array ->
            (0 until array.size()).map { index ->
                val runtime = array.get(index)
                FingerprintRuntime(runtime.path("name").asString(""), runtime.path("vendor").textOrNull(), runtime.path("version").textOrNull())
            }
        }.orEmpty()
        val fingerprint = EnvironmentFingerprint.of(
            osName = tier0.path("os").path("name").textOrNull(),
            arch = tier0.path("os").path("arch").textOrNull(),
            docker = tier0.path("container").path("docker").asBoolean(false),
            wsl = tier0.path("container").path("wsl").asBoolean(false),
            runtimes = runtimes.filter { it.name.isNotBlank() },
        )
        // The client never decides the fingerprint.
        root.put("fingerprint", fingerprint)

        return NormalizedSnapshot(
            schemaVersion = schemaVersion,
            source = source,
            collector = root.path("collector").let { collector ->
                listOfNotNull(collector.path("name").textOrNull(), collector.path("version").textOrNull()).joinToString(" ").ifBlank { null }
            },
            fingerprint = fingerprint,
            serverRedactions = redactions,
            symptom = symptom,
            document = objectMapper.writeValueAsString(root),
        )
    }

    /** Redacts secret-looking values in every string, in place; returns how many it replaced. */
    private fun scrub(node: JsonNode): Int {
        var count = 0
        when (node) {
            is ObjectNode -> node.properties().toList().forEach { (key, value) ->
                if (value is StringNode) {
                    val findings = SecretDetector.find(value.asString()).size
                    if (findings > 0) {
                        node.put(key, SecretDetector.redact(value.asString()))
                        count += findings
                    }
                } else {
                    count += scrub(value)
                }
            }
            is ArrayNode -> (0 until node.size()).forEach { index ->
                val value = node.get(index)
                if (value is StringNode) {
                    val findings = SecretDetector.find(value.asString()).size
                    if (findings > 0) {
                        node.set(index, StringNode.valueOf(SecretDetector.redact(value.asString())))
                        count += findings
                    }
                } else {
                    count += scrub(value)
                }
            }
            else -> Unit
        }
        return count
    }

    private fun JsonNode.textOrNull(): String? = if (isString) asString().takeIf { it.isNotBlank() } else null
}
