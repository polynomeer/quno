package com.quno.qunobackend.domain.stack

import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class VersionTest {
    private fun v(raw: String) = requireNotNull(Version.parseOrNull(raw)) { raw }

    @ParameterizedTest
    @CsvSource(
        "3.2.0, 3.10.0",
        "3.9, 3.10",
        "1.0.0-alpha, 1.0.0-beta",
        "1.0.0-beta, 5.0.0-M1",
        "5.0.0-M1, 5.0.0-M2",
        "5.0.0-M2, 5.0.0-RC1",
        "2.2.0-RC1, 2.2.0-RC2",
        "2.2.0-RC2, 3.0.0-SNAPSHOT",
        "3.0.0-SNAPSHOT, 3.0.0",
        "3.0.0, 3.0.0-sp1",
        "1.0.0-rc.9, 1.0.0-rc.10",
        "4.0.8, 4.0.9",
    )
    fun `orders versions`(lower: String, higher: String) {
        assertTrue(v(lower) < v(higher), "$lower < $higher")
        assertTrue(v(higher) > v(lower), "$higher > $lower")
    }

    @ParameterizedTest
    @CsvSource(
        "3.2, 3.2.0",
        "3.2.0, 3.2.0.0",
        "v1.4.0, 1.4.0",
        "2.0.0.RELEASE, 2.0.0",
        "2.0.0-GA, 2.0.0",
        "1.0.0+build.5, 1.0.0",
        "1.0.0-b2, 1.0.0-beta2",
        "1.0.0-RC1, 1.0.0-cr1",
    )
    fun `treats equivalent spellings as equal`(left: String, right: String) {
        assertEquals(0, v(left).compareTo(v(right)))
        assertEquals(v(left), v(right))
        assertEquals(v(left).hashCode(), v(right).hashCode())
    }

    @Test
    fun `rejects strings that are not a version`() {
        listOf("", "latest", "^3.2.0", "3.x", "1..2", "abc1.0").forEach { assertNull(Version.parseOrNull(it), it) }
    }

    @Test
    fun `reads a declared npm range as its lower bound`() {
        assertEquals(v("3.2.0"), Version.fromDeclaredOrNull("^3.2.0"))
        assertEquals(v("1.4"), Version.fromDeclaredOrNull("~1.4"))
        assertEquals(v("2.0.0"), Version.fromDeclaredOrNull(">=2.0.0"))
        assertNull(Version.fromDeclaredOrNull("*"))
    }
}
