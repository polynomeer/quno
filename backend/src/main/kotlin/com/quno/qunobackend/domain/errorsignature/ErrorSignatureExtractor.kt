package com.quno.qunobackend.domain.errorsignature

import java.security.MessageDigest

/**
 * Turns pasted logs or stack traces into [ErrorSignature]s (Phase 64, ADR-0075). A Kotlin port of
 * experiments/phase-91-mcp/src/signature.js — same rules, same hashes — so the service and the
 * Phase 91 corpus agree on what "the same error" means. Pure: no I/O, safe to call anywhere.
 */
object ErrorSignatureExtractor {
    const val TOP_FRAMES = 5

    /** Inputs beyond this are cut before parsing; a question's logs rarely need more. */
    const val MAX_INPUT_LENGTH = 20_000

    private val HELPFUL_NPE = Regex("""^\s*Cannot (?:invoke|read|load|store|assign|throw|enter|exit)\b""")

    // Class names must start upper-case, so Gradle task names like checkKotlin...Errors don't count.
    private val EXCEPTION = Regex("""(?<![\w$.])((?:[a-z_][\w$]*\.)*[A-Z][\w$]*(?:Exception|Error|Throwable))(?::\s*(.*))?""")
    private val JVM_FRAME = Regex("""^\s*at\s+([\w$.<>]+)\(([^)]*)\)""")
    private val NODE_FRAME = Regex("""^\s*at\s+(?:async\s+)?([^\s(]+)\s+\(.*\)\s*$""")
    private val CAUSED_BY = Regex("""^\s*Caused by:\s*(.*)$""")

    private val UUID = Regex("""\b[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}\b""", RegexOption.IGNORE_CASE)
    private val HEX = Regex("""\b0x[0-9a-f]+\b""", RegexOption.IGNORE_CASE)
    private val IP = Regex("""\b\d{1,3}(?:\.\d{1,3}){3}(?::\d+)?\b""")

    // File paths only: "org/springframework/..." class names follow a word character and are kept.
    private val PATH = Regex("""(?<![\w.$])(?:[A-Za-z]:)?(?:[\\/][\w.@~+-]+){2,}""")
    private val QUOTED = Regex("""'[^']*'|"[^"]*"|`[^`]*`""")
    private val NUMBER = Regex("""\b\d+(?:\.\d+)*\b""")
    private val SPACES = Regex("""\s+""")

    private val PROXY = Regex("""\$\$[A-Za-z]+By[A-Za-z]+\$\$[0-9a-f]+""")
    private val LAMBDA_CLASS = Regex("""\$\${'$'}Lambda[\w$/.]*""")
    private val LAMBDA_METHOD = Regex("""lambda\$([\w$]+)\$\d+""")
    private val ANONYMOUS = Regex("""\$\d+""")

    /** Erases the parts of a message that vary per run: paths, addresses, ids, numbers, quoted names. */
    fun normalizeMessage(message: String?): String {
        val text = message.orEmpty()
        // JDK "helpful NPE" messages quote code names (methods, fields) — stable and telling, so keep them.
        val keepQuoted = HELPFUL_NPE.containsMatchIn(text)
        var out = text
            .replace(UUID, "<uuid>")
            .replace(HEX, "<hex>")
            .replace(IP, "<ip>")
            .replace(PATH, "<path>")
        if (!keepQuoted) out = out.replace(QUOTED, "<str>")
        return out
            .replace(NUMBER, "<n>")
            .replace(SPACES, " ")
            .trim()
            .lowercase()
    }

    /** Drops proxy suffixes, lambda numbering and anonymous-class numbers from a frame's method. */
    // Lambda replacements, not strings: a "$" in a replacement string is a group reference.
    fun normalizeFrame(method: String): String = method
        .replace(PROXY) { "\$proxy" }
        .replace(LAMBDA_CLASS) { "\$lambda" }
        .replace(LAMBDA_METHOD) { "lambda\$${it.groupValues[1]}" }
        .replace(ANONYMOUS) { "\$n" }

    /**
     * Every exception chain in [text], one signature per distinct chain (by root cause). People paste
     * whole build logs, which mix unrelated warning traces in, so callers search with all of them.
     */
    fun extractAll(text: String?): List<ErrorSignature> {
        if (text.isNullOrBlank()) return emptyList()
        val seen = mutableSetOf<String>()
        return parseChains(text.take(MAX_INPUT_LENGTH))
            .map { toSignature(it) }
            .filter { seen.add(it.messageKey + "|" + (it.signatureHash ?: "")) }
    }

    /** The first chain's signature, or null when the text holds no exception. */
    fun extract(text: String?): ErrorSignature? = extractAll(text).firstOrNull()

    private class Block(val exceptionClass: String, val message: String) {
        val frames = mutableListOf<String>()
    }

    // "Caused by:" continues the current chain; any other non-frame line naming an exception starts a new one.
    private fun parseChains(text: String): List<List<Block>> {
        val chains = mutableListOf<MutableList<Block>>()
        var current: MutableList<Block>? = null
        for (line in text.lines()) {
            val frame = JVM_FRAME.find(line)?.groupValues?.get(1) ?: NODE_FRAME.find(line)?.groupValues?.get(1)
            if (frame != null) {
                current?.last()?.frames?.add(normalizeFrame(frame))
                continue
            }
            val causedBy = CAUSED_BY.find(line)
            val match = EXCEPTION.find(causedBy?.groupValues?.get(1) ?: line) ?: continue
            val block = Block(match.groupValues[1], match.groups[2]?.value.orEmpty())
            if (causedBy != null && current != null) {
                current.add(block)
            } else {
                current = mutableListOf(block)
                chains.add(current)
            }
        }
        return chains
    }

    private fun toSignature(blocks: List<Block>): ErrorSignature {
        val root = blocks.last()
        val message = normalizeMessage(root.message)
        val frames = root.frames.take(TOP_FRAMES)
        return ErrorSignature(
            exceptionClass = root.exceptionClass,
            normalizedMessage = message,
            frames = frames,
            signatureHash = if (frames.isEmpty()) null else sha("${root.exceptionClass}|${frames.joinToString("|")}"),
            messageKey = sha("${root.exceptionClass}|$message"),
            chain = blocks.map { it.exceptionClass },
        )
    }

    private fun sha(value: String): String = MessageDigest.getInstance("SHA-256")
        .digest(value.toByteArray(Charsets.UTF_8))
        .joinToString("") { "%02x".format(it) }
        .take(16)
}
