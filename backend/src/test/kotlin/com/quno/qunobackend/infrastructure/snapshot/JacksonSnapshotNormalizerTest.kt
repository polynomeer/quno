package com.quno.qunobackend.infrastructure.snapshot

import com.quno.qunobackend.domain.snapshot.InvalidEnvironmentSnapshotException
import com.quno.qunobackend.domain.snapshot.SnapshotSymptom
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import tools.jackson.databind.json.JsonMapper
import tools.jackson.databind.node.ObjectNode
import java.io.File
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class JacksonSnapshotNormalizerTest {
    private val mapper = JsonMapper.builder().build()
    private val normalizer = JacksonSnapshotNormalizer(mapper)

    private data class CorpusEntry(val label: String, val value: String, val context: String)

    /** Shared with the CLI collector's test (Phase 93.7, ADR-0084). */
    private val corpus: List<CorpusEntry> = mapper.readTree(File("../testdata/secret-leak-corpus.json")).path("entries").let { entries ->
        (0 until entries.size()).map { index ->
            val entry = entries.get(index)
            CorpusEntry(entry.path("label").asString(), entry.path("value").asString(), entry.path("context").asString())
        }
    }

    private fun document(build: ObjectNode.() -> Unit = {}): String {
        val root = mapper.createObjectNode()
        root.put("schemaVersion", 1)
        root.put("source", "COLLECTOR")
        root.putObject("tier0").putObject("os").put("name", "macOS").put("arch", "arm64")
        root.build()
        return mapper.writeValueAsString(root)
    }

    @Test
    fun `no corpus secret survives, wherever in the document it was planted`() {
        val raw = document {
            putArray("tiers").add(0).add(1).add(2)
            withObject("tier0").putArray("jvmArgs").apply { corpus.forEach { add(it.context) } }
            putObject("tier1").apply {
                putObject("command").putArray("argv").apply { corpus.forEach { add(it.context) } }
                putArray("env").apply { corpus.forEach { addObject().put("name", "X").put("value", it.context) } }
                putArray("failedTests").apply { corpus.forEach { addObject().put("name", "t").put("message", it.context) } }
            }
            putObject("tier2").putObject("unknownFutureField").put("nested", corpus.joinToString("\n") { it.context })
        }

        val normalized = normalizer.normalize(raw)

        val leaked = corpus.filter { normalized.document.contains(it.value) }.map { it.label }
        assertEquals(emptyList(), leaked, "secrets left in the stored document")
        assertTrue(normalized.serverRedactions >= corpus.size * 4)
    }

    @Test
    fun `reads the symptom, and keeps an unknown one as null instead of refusing`() {
        assertEquals(
            SnapshotSymptom.OUT_OF_MEMORY,
            normalizer.normalize(
                document {
                    putArray("tiers").add(0).add(1)
                    putObject("tier1").put("symptom", "OUT_OF_MEMORY")
                },
            ).symptom,
        )
        assertNull(
            normalizer.normalize(
                document {
                    putArray("tiers").add(0).add(1)
                    putObject("tier1").put("symptom", "SOLAR_FLARE")
                },
            ).symptom,
        )
        assertNull(normalizer.normalize(document()).symptom)
    }

    @Test
    fun `refuses unknown tiers and tier2 data without the opt-in marker`() {
        assertThrows<InvalidEnvironmentSnapshotException> { normalizer.normalize(document { putArray("tiers").add(0).add(3) }) }
        assertThrows<InvalidEnvironmentSnapshotException> {
            normalizer.normalize(
                document {
                    putArray("tiers").add(0).add(1)
                    putObject("tier2").put("cpus", 8)
                },
            )
        }
        val optedIn = normalizer.normalize(
            document {
                putArray("tiers").add(0).add(2)
                putObject("tier2").put("cpus", 8)
            },
        )
        assertFalse(optedIn.document.contains("[REDACTED]"))
    }
}
