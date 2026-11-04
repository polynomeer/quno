package com.quno.qunobackend.domain.snapshot

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class EnvironmentRecipeTest {
    private val java = FingerprintRuntime("java", "Eclipse Adoptium", "21.0.5")

    private fun recipe(os: String?, arch: String? = "x86_64", docker: Boolean = false, runtimes: List<FingerprintRuntime> = listOf(java)) = EnvironmentRecipe.from(EnvironmentCore(os, arch, docker, false, runtimes))

    @Test
    fun `picks the nearest hosted runner`() {
        assertEquals("macos-latest", recipe("macOS", "arm64").runsOn)
        assertEquals("macos-13", recipe("macOS", "x86_64").runsOn)
        assertEquals("windows-latest", recipe("Windows 11").runsOn)
        assertEquals("ubuntu-24.04-arm", recipe("Linux", "aarch64").runsOn)
        assertEquals("ubuntu-latest", recipe("Linux").runsOn)
    }

    @Test
    fun `reads major versions and says what it could not match`() {
        val recipe = recipe("Linux", docker = true, runtimes = listOf(FingerprintRuntime("java", "Azul", "1.8.0_412"), FingerprintRuntime("node", null, "v22.11.0")))
        assertEquals("8", recipe.javaVersion)
        assertEquals("22", recipe.nodeVersion)
        assertTrue(recipe.notes.any { it.contains("Docker") })
        assertTrue(recipe.notes.any { it.contains("Azul") })
        assertEquals(emptyList(), recipe("Linux").notes)
    }

    @Test
    fun `the label leaves patch versions out, like the fingerprint`() {
        assertEquals("Linux x86_64 · Docker · java 21", EnvironmentCore("Linux", "x86_64", true, false, listOf(java)).label)
    }
}
