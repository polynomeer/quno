package com.quno.qunobackend.domain.engine

/** One cleanup the tidy engine would make to a question body (Phase 78.5, ADR-0082). */
data class TidyProposal(
    val kind: EngineSuggestionKind,
    val summary: String,
    val body: String,
    /** Set only by MOVE_LONG_LOG. */
    val logs: String? = null,
)

/**
 * Formatting suggestions for question bodies: fences that lack a language, stack traces pasted
 * outside a fence, and long logs that belong in the logs field. Pure and deterministic like
 * [SecretDetector]; each proposal starts from the same body, so accepting one supersedes the rest
 * and the next analysis offers what still applies.
 */
object FormatTidy {
    private const val LONG_LOG_LINES = 30
    private const val MIN_TRACE_FRAMES = 3

    private val FENCE = Regex("""(?m)^(```|~~~)([^\n`]*)\n([\s\S]*?)\n\1[ \t]*$""")
    private val FRAME = Regex("""^\s*at [\w$.<>/\-]+\(.*\)\s*$""")
    private val TRACE_HEAD = Regex("""^\s*(?:Caused by: |Exception in thread "[^"]*" )?[\w$.]+(?:Exception|Error|Throwable)(?::.*)?$""")
    private val TRACE_TAIL = Regex("""^\s*(?:\.\.\. \d+ more|Caused by: .*|Suppressed: .*)$""")

    fun analyze(body: String, logs: String?): List<TidyProposal> = listOfNotNull(moveLongLog(body, logs), fenceStackTrace(body), addFenceLanguage(body))

    /** A fenced block of 30+ lines that reads as a log, when the logs field is still empty. */
    fun moveLongLog(body: String, logs: String?): TidyProposal? {
        if (!logs.isNullOrBlank()) return null
        val block = FENCE.findAll(body).firstOrNull { match ->
            val lines = match.groupValues[3].lines()
            lines.size >= LONG_LOG_LINES && looksLikeLog(lines)
        } ?: return null
        val remaining = (body.substring(0, block.range.first) + body.substring(block.range.last + 1)).replace(Regex("""\n{3,}"""), "\n\n").trim()
        if (remaining.isBlank()) return null
        return TidyProposal(
            kind = EngineSuggestionKind.MOVE_LONG_LOG,
            summary = "본문의 긴 로그(${block.groupValues[3].lines().size}줄)를 로그 칸으로 옮기면 질문이 짧아지고 로그는 접혀 보여요.",
            body = remaining,
            logs = block.groupValues[3],
        )
    }

    /** Runs of 3+ stack frames outside any fence get wrapped in a ```text fence. */
    fun fenceStackTrace(body: String): TidyProposal? {
        val lines = body.lines()
        val fenced = fencedLineIndexes(body)
        val runs = mutableListOf<IntRange>()
        var i = 0
        while (i < lines.size) {
            if (i !in fenced && FRAME.matches(lines[i])) {
                var start = i
                var end = i
                while (end + 1 < lines.size && end + 1 !in fenced && (FRAME.matches(lines[end + 1]) || TRACE_TAIL.matches(lines[end + 1]))) end++
                val frames = (start..end).count { FRAME.matches(lines[it]) }
                if (start > 0 && start - 1 !in fenced && TRACE_HEAD.matches(lines[start - 1])) start--
                if (frames >= MIN_TRACE_FRAMES) runs += start..end
                i = end + 1
            } else {
                i++
            }
        }
        if (runs.isEmpty()) return null
        val out = mutableListOf<String>()
        lines.forEachIndexed { index, line ->
            if (runs.any { it.first == index }) out += "```text"
            out += line
            if (runs.any { it.last == index }) out += "```"
        }
        return TidyProposal(
            kind = EngineSuggestionKind.FENCE_STACK_TRACE,
            summary = "코드 블록 밖에 있는 스택트레이스 ${runs.size}곳을 코드 블록으로 감싸면 줄바꿈이 유지되고 읽기 쉬워져요.",
            body = out.joinToString("\n"),
        )
    }

    /** Fences without an info string whose content clearly reads as one language. */
    fun addFenceLanguage(body: String): TidyProposal? {
        val labelled = mutableListOf<String>()
        val result = FENCE.replace(body) { match ->
            val (marker, info, content) = match.destructured
            val language = if (info.isBlank()) detectLanguage(content) else null
            if (language == null) {
                match.value
            } else {
                labelled += language
                "$marker$language\n$content\n$marker"
            }
        }
        if (labelled.isEmpty()) return null
        return TidyProposal(
            kind = EngineSuggestionKind.ADD_CODE_FENCE_LANGUAGE,
            summary = "언어가 없는 코드 블록 ${labelled.size}개에 언어(${labelled.distinct().joinToString()})를 붙이면 문법 강조가 돼요.",
            body = result,
        )
    }

    /** Null when unsure — a wrong label is worse than none. */
    fun detectLanguage(content: String): String? {
        val text = content.trim()
        if (text.isEmpty()) return null
        val lines = text.lines()
        return when {
            lines.count { FRAME.matches(it) } >= 2 -> "text"
            (text.startsWith("{") && text.endsWith("}") || text.startsWith("[") && text.endsWith("]")) && Regex(""""[^"]+"\s*:""").containsMatchIn(text) -> "json"
            text.startsWith("<") && text.endsWith(">") && Regex("""</?[\w:.\-]+""").containsMatchIn(text) -> "xml"
            Regex("""(?i)^\s*(select|insert\s+into|update|delete\s+from|create\s+(table|index)|alter\s+table|with)\b""").containsMatchIn(text) && Regex("""(?i)\b(from|into|set|table|values|where)\b""").containsMatchIn(text) -> "sql"
            Regex("""(?m)^\s*(plugins|dependencies|repositories)\s*\{""").containsMatchIn(text) && Regex("""(?m)^\s*(implementation|id|testImplementation|kotlin)\s*\(""").containsMatchIn(text) -> "kotlin"
            Regex("""(?m)^\s*(fun |val |var |data class |suspend fun |object |@\w+\s*$)""").containsMatchIn(text) && !text.contains("public class") -> "kotlin"
            Regex("""(?m)^\s*(public |private |protected )?(static )?(final )?(class|interface|record|enum) \w+""").containsMatchIn(text) && lines.count { it.trimEnd().endsWith(";") } >= 1 -> "java"
            lines.all { it.isBlank() || it.trimStart().startsWith("#") || Regex("""^\s*(- )?[\w.\-"']+\s*:(\s|$)""").containsMatchIn(it) || it.startsWith("  ") } &&
                lines.count { Regex("""^\s*[\w.\-]+\s*:""").containsMatchIn(it) } >= 2 &&
                !text.contains("{") -> "yaml"
            lines.all { it.isBlank() || Regex("""^\s*(\$ |#|(\./)?gradlew|npm |npx |yarn |pnpm |docker |kubectl |curl |git |cd |export |sudo |brew |mvn |java |node )""").containsMatchIn(it) } -> "bash"
            else -> null
        }
    }

    private fun looksLikeLog(lines: List<String>): Boolean {
        val timestamped = lines.count { Regex("""^\s*\[?\d{4}-\d{2}-\d{2}[ T]\d{2}:\d{2}""").containsMatchIn(it) }
        val frames = lines.count { FRAME.matches(it) }
        return (timestamped + frames) * 2 >= lines.size
    }

    private fun fencedLineIndexes(body: String): Set<Int> {
        val indexes = mutableSetOf<Int>()
        val lines = body.lines()
        var open: String? = null
        lines.forEachIndexed { index, line ->
            val marker = Regex("""^(```|~~~)""").find(line)?.value
            when {
                open == null && marker != null -> {
                    open = marker
                    indexes += index
                }
                open != null -> {
                    indexes += index
                    if (line.trimEnd() == open) open = null
                }
            }
        }
        return indexes
    }
}
