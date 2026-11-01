package com.quno.qunobackend.domain.engine

import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class FormatTidyTest {
    private val fence = "```"

    @ParameterizedTest
    @CsvSource(
        delimiter = '|',
        value = [
            """{"a": 1, "b": [true]}|json""",
            """<dependency><artifactId>x</artifactId></dependency>|xml""",
            """SELECT id FROM questions WHERE status = 'OPEN'|sql""",
            """fun main() {\n    println("hi")\n}|kotlin""",
            """public class Main {\n  int x = 1;\n}|java""",
            """spring:\n  datasource:\n    url: jdbc:postgresql://db/quno|yaml""",
            """./gradlew test --info\ndocker compose up -d|bash""",
            """plugins {\n    id("org.springframework.boot") version "4.0.8"\n}\ndependencies {\n    implementation("a:b:1")\n}|kotlin""",
        ],
    )
    fun `detects a language only when the content clearly reads as one`(content: String, language: String) {
        assertEquals(language, FormatTidy.detectLanguage(content.replace("\\n", "\n")))
    }

    @Test
    fun `stays silent when unsure`() {
        assertNull(FormatTidy.detectLanguage("just some words here"))
        assertNull(FormatTidy.detectLanguage("x = 1"))
        assertNull(FormatTidy.detectLanguage(""))
    }

    @Test
    fun `labels only unlabelled fences`() {
        val body = "Config:\n$fence\n{\"port\": 8080}\n$fence\n\nCode:\n${fence}kotlin\nval x = 1\n$fence\n\n$fence\nhello there\n$fence"
        val proposal = FormatTidy.addFenceLanguage(body)!!

        assertEquals(EngineSuggestionKind.ADD_CODE_FENCE_LANGUAGE, proposal.kind)
        assertEquals(body.replaceFirst("$fence\n{", "${fence}json\n{"), proposal.body)
        assertNull(FormatTidy.addFenceLanguage(proposal.body), "nothing left to label")
    }

    @Test
    fun `wraps unfenced traces with their exception line, and leaves fenced ones alone`() {
        val trace = listOf(
            "java.lang.IllegalStateException: boom",
            "\tat demo.Service.run(Service.kt:10)",
            "\tat demo.Main.main(Main.kt:3)",
            "Caused by: java.io.IOException: closed",
            "\tat demo.Io.read(Io.kt:5)",
            "\t... 2 more",
        ).joinToString("\n")
        val proposal = FormatTidy.fenceStackTrace("It fails:\n$trace\nAny idea?")!!

        assertEquals("It fails:\n${fence}text\n$trace\n$fence\nAny idea?", proposal.body)
        assertNull(FormatTidy.fenceStackTrace(proposal.body), "fenced now")
        assertNull(FormatTidy.fenceStackTrace("one frame\n\tat a.B.c(B.kt:1)\n\tat a.B.d(B.kt:2)"), "two frames is not a trace yet")
    }

    @Test
    fun `moves a long log out of the body only while the logs field is empty`() {
        val log = (1..35).joinToString("\n") { "2026-11-01 10:00:${"%02d".format(it % 60)} INFO line $it" }
        val body = "Startup hangs.\n\n$fence\n$log\n$fence\n\nWhat am I missing?"

        val proposal = FormatTidy.moveLongLog(body, null)!!
        assertEquals("Startup hangs.\n\nWhat am I missing?", proposal.body)
        assertEquals(log, proposal.logs)
        assertNull(FormatTidy.moveLongLog(body, "already has logs"))
        assertNull(FormatTidy.moveLongLog("$fence\n$log\n$fence", null), "the body would be empty")
        assertNull(FormatTidy.moveLongLog("x\n$fence\n${(1..35).joinToString("\n") { "val v$it = $it" }}\n$fence", null), "long code is not a log")
    }

    @Test
    fun `analyze offers each applicable cleanup from the same body`() {
        val body = "Fails:\njava.lang.Error: x\n\tat a.B.c(B.kt:1)\n\tat a.B.d(B.kt:2)\n\tat a.B.e(B.kt:3)\n\n$fence\n{\"a\": 1}\n$fence"
        val kinds = FormatTidy.analyze(body, null).map { it.kind }

        assertEquals(listOf(EngineSuggestionKind.FENCE_STACK_TRACE, EngineSuggestionKind.ADD_CODE_FENCE_LANGUAGE), kinds)
        assertTrue(FormatTidy.analyze("Plain question with nothing to tidy.", null).isEmpty())
    }
}
