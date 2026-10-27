package com.quno.qunobackend.domain.stack

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ApplicabilityRangeTest {
    private val boot = "org.springframework.boot"
    private fun v(raw: String) = requireNotNull(Version.parseOrNull(raw))

    @Test
    fun `from is inclusive and until is exclusive`() {
        val range = ApplicabilityRange(Ecosystem.GRADLE_PLUGIN, boot, "3.2.0", "4.0.0")

        assertFalse(v("3.1.9") in range)
        assertTrue(v("3.2.0") in range)
        assertTrue(v("3.2") in range)
        assertTrue(v("3.99.99") in range)
        assertTrue(v("4.0.0-RC1") in range, "pre-releases sort below their release, as in Maven and semver")
        assertFalse(v("4.0.0") in range)
    }

    @Test
    fun `a single bound leaves the other side open`() {
        assertTrue(v("99.0") in ApplicabilityRange(Ecosystem.NPM, "react", "18.0.0", null))
        assertTrue(v("0.1") in ApplicabilityRange(Ecosystem.NPM, "react", null, "19.0.0"))
        assertFalse(v("19.0.0") in ApplicabilityRange(Ecosystem.NPM, "react", null, "19.0.0"))
    }

    @Test
    fun `rejects ranges without a bound, with an unreadable bound, or in the wrong order`() {
        assertThrows<InvalidApplicabilityRangeException> { ApplicabilityRange(Ecosystem.NPM, "react", null, null) }
        assertThrows<InvalidApplicabilityRangeException> { ApplicabilityRange(Ecosystem.NPM, "react", "^18", null) }
        assertThrows<InvalidApplicabilityRangeException> { ApplicabilityRange(Ecosystem.NPM, "react", "19.0", "18.0") }
        assertThrows<InvalidApplicabilityRangeException> { ApplicabilityRange(Ecosystem.NPM, "react", "18.0", "18.0.0") }
        assertThrows<InvalidApplicabilityRangeException> { ApplicabilityRange(Ecosystem.NPM, " ", "18.0", null) }
    }

    @Test
    fun `package patterns match exactly or by a trailing star`() {
        val exact = PackagePattern(Ecosystem.GRADLE_PLUGIN, boot)
        val prefix = PackagePattern(Ecosystem.MAVEN, "org.springframework.boot:spring-boot*")

        assertTrue(exact.matches(Ecosystem.GRADLE_PLUGIN, boot))
        assertFalse(exact.matches(Ecosystem.MAVEN, boot))
        assertFalse(exact.matches(Ecosystem.GRADLE_PLUGIN, "$boot.extra"))
        assertTrue(prefix.matches(Ecosystem.MAVEN, "org.springframework.boot:spring-boot-starter-web"))
        assertFalse(prefix.matches(Ecosystem.MAVEN, "org.springframework:spring-core"))
    }

    @Test
    fun `verdict falls back to NO_RANGE, skips covered versions and flags the rest`() {
        val patterns = listOf(PackagePattern(Ecosystem.GRADLE_PLUGIN, boot))
        val covering = ApplicabilityRange(Ecosystem.GRADLE_PLUGIN, boot, "3.2.0", "4.0.0")
        val older = ApplicabilityRange(Ecosystem.GRADLE_PLUGIN, boot, null, "3.0.0")
        val unrelated = ApplicabilityRange(Ecosystem.NPM, "react", "18.0", null)

        assertEquals(ApplicabilityVerdict.NO_RANGE, ApplicabilityVerdict.of(emptyList(), patterns, "3.5.0"))
        assertEquals(ApplicabilityVerdict.NO_RANGE, ApplicabilityVerdict.of(listOf(unrelated), patterns, "3.5.0"))
        assertEquals(ApplicabilityVerdict.NO_RANGE, ApplicabilityVerdict.of(listOf(covering), emptyList(), "3.5.0"))
        assertEquals(ApplicabilityVerdict.NO_RANGE, ApplicabilityVerdict.of(listOf(covering), patterns, "not-a-version"))
        assertEquals(ApplicabilityVerdict.IN_RANGE, ApplicabilityVerdict.of(listOf(older, covering), patterns, "3.5.0"))
        assertEquals(ApplicabilityVerdict.OUT_OF_RANGE, ApplicabilityVerdict.of(listOf(older, covering), patterns, "4.0.0"))
    }
}
