package com.quno.qunobackend.domain.engine

import java.security.MessageDigest
import kotlin.math.ln

/** One secret-looking span in a text; [valueHash] identifies the value without keeping it (ADR-0081). */
data class SecretFinding(val label: String, val start: Int, val end: Int, val valueHash: String)

/**
 * Finds keys, tokens and similar values pasted into questions (Phase 78.4, ADR-0081). Pure and
 * deterministic, so masking can run on every read and a rule change applies to old versions at once.
 * Leans towards precision: generic `password=` assignments need an entropy floor so that sample
 * values like `changeme` stay readable.
 */
object SecretDetector {
    private data class Rule(val label: String, val regex: Regex, val group: Int = 0, val accept: (text: String, range: IntRange) -> Boolean = { _, _ -> true })

    private val RULES = listOf(
        Rule("개인 키", Regex("""-----BEGIN [A-Z ]*PRIVATE KEY-----[\s\S]*?-----END [A-Z ]*PRIVATE KEY-----""")),
        Rule("AWS 액세스 키", Regex("""\b(?:AKIA|ASIA)[0-9A-Z]{16}\b""")),
        Rule("GitHub 토큰", Regex("""\b(?:gh[pousr]_[A-Za-z0-9]{36,}|github_pat_[A-Za-z0-9_]{60,})\b""")),
        Rule("Slack 토큰", Regex("""\bxox[abposr]-[A-Za-z0-9-]{10,}""")),
        Rule("Stripe 키", Regex("""\b(?:sk|rk)_live_[A-Za-z0-9]{16,}\b""")),
        Rule("Google API 키", Regex("""\bAIza[0-9A-Za-z_\-]{35}\b""")),
        Rule("Quno 액세스 토큰", Regex("""\bquno_pat_[A-Za-z0-9_\-]{20,}""")),
        Rule("JWT", Regex("""\beyJ[A-Za-z0-9_\-]{8,}\.eyJ[A-Za-z0-9_\-]{8,}\.[A-Za-z0-9_\-]{8,}""")),
        Rule("URL 속 비밀번호", Regex("""\b[a-zA-Z][a-zA-Z0-9+.\-]*://[^\s:/@]+:([^\s@/]{3,})@"""), group = 1),
        Rule(
            "비밀값",
            Regex("""(?i)\b[\w.\-]*(?:password|passwd|pwd|secret|token|api[_\-]?key|access[_\-]?key|client[_\-]?secret)["']?\s*[:=]\s*["']?([^\s"',;]{8,})"""),
            group = 1,
            accept = { text, range ->
                val value = text.substring(range)
                !value.startsWith("\${") && !value.startsWith("<") && entropy(value) >= 3.0
            },
        ),
        // Four-part versions (jackson 2.13.4.2) look like addresses, so an IP only counts in network context.
        Rule("공인 IP 주소", Regex("""(?<![\w.:\-])(?:\d{1,3}\.){3}\d{1,3}(?![\w\-]|\.\d)"""), accept = { text, range -> isPublicIpv4(text.substring(range)) && inNetworkContext(text, range) }),
    )

    fun find(text: String?): List<SecretFinding> {
        if (text.isNullOrEmpty()) return emptyList()
        val findings = mutableListOf<SecretFinding>()
        RULES.forEach { rule ->
            rule.regex.findAll(text).forEach { match ->
                val range = match.groups[rule.group]?.range ?: return@forEach
                if (rule.accept(text, range) && findings.none { it.start <= range.last && range.first < it.end }) {
                    findings += SecretFinding(rule.label, range.first, range.last + 1, hash(text.substring(range)))
                }
            }
        }
        return findings.sortedBy { it.start }
    }

    /** Replaces every finding not in [allowed] (value hashes the author marked as "not a secret"). */
    fun mask(text: String?, allowed: Set<String> = emptySet()): String? {
        if (text == null) return null
        val findings = find(text).filter { it.valueHash !in allowed }
        if (findings.isEmpty()) return text
        val builder = StringBuilder(text)
        findings.sortedByDescending { it.start }.forEach { builder.replace(it.start, it.end, "[가려짐: ${it.label}]") }
        return builder.toString()
    }

    /** What accepting a redaction suggestion writes into the new revision. */
    fun redact(text: String?, allowed: Set<String> = emptySet()): String? {
        if (text == null) return null
        val findings = find(text).filter { it.valueHash !in allowed }
        if (findings.isEmpty()) return text
        val builder = StringBuilder(text)
        findings.sortedByDescending { it.start }.forEach { builder.replace(it.start, it.end, REDACTED) }
        return builder.toString()
    }

    const val REDACTED = "[REDACTED]"

    fun hash(value: String): String = MessageDigest.getInstance("SHA-256").digest(value.toByteArray()).joinToString("") { "%02x".format(it) }

    /** Shannon entropy in bits per character. */
    fun entropy(value: String): Double {
        if (value.isEmpty()) return 0.0
        return value.groupingBy { it }.eachCount().values.sumOf { count ->
            val p = count.toDouble() / value.length
            -p * ln(p) / ln(2.0)
        }
    }

    private val NETWORK_WORDS = Regex("""(?i)\b(ip|host|hostname|addr|address|server|connect(?:ed|ing|ion)?|refused|endpoint|remote|peer|client)\W*(?:(?:to|from|on|at)\W*)?$""")

    private fun inNetworkContext(text: String, range: IntRange): Boolean {
        if (Regex("""^:\d{1,5}\b""").containsMatchIn(text.substring(range.last + 1).take(6))) return true
        val before = text.substring(maxOf(0, range.first - 24), range.first)
        return before.endsWith("//") || before.endsWith("@") || NETWORK_WORDS.containsMatchIn(before)
    }

    private fun isPublicIpv4(text: String): Boolean {
        val octets = text.split('.').map { it.toIntOrNull() ?: return false }
        if (octets.any { it !in 0..255 }) return false
        val (a, b, c) = octets
        return !(
            a == 0 ||
                a == 10 ||
                a == 127 ||
                a >= 224 ||
                (a == 100 && b in 64..127) ||
                (a == 169 && b == 254) ||
                (a == 172 && b in 16..31) ||
                (a == 192 && b == 168) ||
                (a == 192 && b == 0 && c in setOf(0, 2)) ||
                (a == 198 && b in 18..19) ||
                (a == 198 && b == 51 && c == 100) ||
                (a == 203 && b == 0 && c == 113)
            )
    }
}
