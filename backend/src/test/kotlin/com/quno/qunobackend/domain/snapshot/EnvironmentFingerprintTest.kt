package com.quno.qunobackend.domain.snapshot

import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

class EnvironmentFingerprintTest {
    private fun fp(jdk: String, arch: String = "arm64", docker: Boolean = false, os: String = "macOS") = EnvironmentFingerprint.of(os, arch, docker, wsl = false, runtimes = listOf(FingerprintRuntime("java", "Temurin", jdk)))

    @ParameterizedTest
    @CsvSource("21.0.5,21", "1.8.0_412,8", "v22.11.0,22", "17,17", "'',?")
    fun `reads the major version`(version: String, major: String) {
        assertEquals(major, EnvironmentFingerprint.major(version))
    }

    @Test
    fun `groups patch versions and case differences, separates majors, architectures and containers`() {
        assertEquals(fp("21.0.5"), fp("21.0.9"))
        assertEquals(fp("21.0.5"), fp("21.0.5", os = "MACOS "))
        assertNotEquals(fp("21.0.5"), fp("17.0.12"))
        assertNotEquals(fp("21.0.5"), fp("21.0.5", arch = "x86_64"))
        assertNotEquals(fp("21.0.5"), fp("21.0.5", docker = true))
        assertEquals(16, fp("21").length)
    }

    @Test
    fun `runtime order does not matter`() {
        val java = FingerprintRuntime("java", "Temurin", "21.0.5")
        val node = FingerprintRuntime("node", null, "22.11.0")
        assertEquals(
            EnvironmentFingerprint.of("Linux", "x86_64", true, false, listOf(java, node)),
            EnvironmentFingerprint.of("Linux", "x86_64", true, false, listOf(node, java)),
        )
    }
}
