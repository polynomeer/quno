package com.quno.qunobackend.domain.question

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class TextDiffTest {

    @Test
    fun `identical text produces only EQUAL lines`() {
        val diff = TextDiffer.diffLines("a\nb\nc", "a\nb\nc")

        assertEquals(listOf(DiffLineType.EQUAL, DiffLineType.EQUAL, DiffLineType.EQUAL), diff.map { it.type })
    }

    @Test
    fun `an appended line shows up as ADDED`() {
        val diff = TextDiffer.diffLines("a\nb", "a\nb\nc")

        assertEquals(
            listOf(
                DiffLine(DiffLineType.EQUAL, "a"),
                DiffLine(DiffLineType.EQUAL, "b"),
                DiffLine(DiffLineType.ADDED, "c"),
            ),
            diff,
        )
    }

    @Test
    fun `a removed line shows up as REMOVED`() {
        val diff = TextDiffer.diffLines("a\nb\nc", "a\nc")

        assertEquals(
            listOf(
                DiffLine(DiffLineType.EQUAL, "a"),
                DiffLine(DiffLineType.REMOVED, "b"),
                DiffLine(DiffLineType.EQUAL, "c"),
            ),
            diff,
        )
    }

    @Test
    fun `a changed line shows up as one REMOVED and one ADDED`() {
        val diff = TextDiffer.diffLines("Spring Boot 3.x", "Spring Boot 4.x")

        assertEquals(
            listOf(
                DiffLine(DiffLineType.REMOVED, "Spring Boot 3.x"),
                DiffLine(DiffLineType.ADDED, "Spring Boot 4.x"),
            ),
            diff,
        )
    }
}
