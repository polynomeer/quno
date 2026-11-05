package com.quno.qunobackend.domain.reproduction

import com.quno.qunobackend.domain.reproduction.ReproductionResult.BUILD_FAILED
import com.quno.qunobackend.domain.reproduction.ReproductionResult.NOT_REPRODUCED
import com.quno.qunobackend.domain.reproduction.ReproductionResult.REPRODUCED
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class VersionBisectTest {
    private val versions = (0..20).map { "1.$it.0" }

    private fun evaluate(observations: List<BisectObservation>, candidates: List<String> = versions) = VersionBisect.evaluate(candidates, NOT_REPRODUCED, REPRODUCED, observations)

    /** Drives the search like the workflow does, against a behaviour function. */
    private fun search(behaviour: (String) -> ReproductionResult, candidates: List<String> = versions): BisectState {
        val seen = mutableListOf<BisectObservation>()
        repeat(20) {
            val state = evaluate(seen, candidates)
            if (state.status != BisectStatus.RUNNING) return state
            state.probes.forEach { seen += BisectObservation(it, behaviour(it)) }
        }
        error("did not settle: $seen")
    }

    @Test
    fun `finds the first bad version within the budget and confirms both sides`() {
        val state = search({ if (it.split('.')[1].toInt() >= 13) REPRODUCED else NOT_REPRODUCED })
        assertEquals(BisectStatus.FOUND, state.status)
        assertEquals("1.12.0", state.low)
        assertEquals("1.13.0", state.high)
        assertEquals(BisectConfidence.HIGH, state.confidence)
        assertTrue(state.runsUsed <= VersionBisect.MAX_RUNS)
    }

    @Test
    fun `probes spread over the range rather than one at a time`() {
        assertEquals(listOf("1.5.0", "1.10.0", "1.15.0"), evaluate(emptyList()).probes)
    }

    @Test
    fun `neighbouring ends only need confirming`() {
        val pair = listOf("1.0.0", "1.1.0")
        assertEquals(listOf("1.0.0", "1.1.0"), evaluate(emptyList(), pair).probes)
        assertEquals(BisectConfidence.HIGH, search({ if (it == "1.1.0") REPRODUCED else NOT_REPRODUCED }, pair).confidence)
    }

    @Test
    fun `versions that give no answer are skipped, and only those left unresolved`() {
        val state = search({
            val minor = it.split('.')[1].toInt()
            when {
                minor in 8..12 -> BUILD_FAILED
                minor >= 13 -> REPRODUCED
                else -> NOT_REPRODUCED
            }
        })
        assertEquals(BisectStatus.UNRESOLVED, state.status)
        assertEquals("1.7.0", state.low)
        assertEquals("1.13.0", state.high)
    }

    @Test
    fun `behaviour that flips back lowers the confidence`() {
        val state = search({
            val minor = it.split('.')[1].toInt()
            if (minor in 5..6 || minor == 20) REPRODUCED else NOT_REPRODUCED
        })
        assertEquals(BisectStatus.FOUND, state.status)
        assertEquals(BisectConfidence.LOW, state.confidence)
    }

    @Test
    fun `a flaky side shows in the confidence`() {
        val pair = listOf("1.0.0", "1.1.0")
        val state = evaluate(listOf(BisectObservation("1.0.0", NOT_REPRODUCED), BisectObservation("1.1.0", NOT_REPRODUCED), BisectObservation("1.1.0", REPRODUCED)), pair)
        assertEquals(BisectStatus.FOUND, state.status)
        assertEquals(BisectConfidence.LOW, state.confidence)
    }

    @Test
    fun `the budget stops the search and says what range it narrowed to`() {
        val many = (0..200).map { "1.$it.0" }
        val failing = List(VersionBisect.MAX_RUNS) { BisectObservation("1.${it + 1}.0", BUILD_FAILED) }
        val state = evaluate(failing, many)
        assertEquals(BisectStatus.BUDGET_REACHED, state.status)
        assertEquals("1.0.0", state.low)
        assertEquals("1.200.0", state.high)
    }

    @Test
    fun `candidates keep stable releases inside the range, ends included`() {
        val released = listOf("0.9.0", "1.0.0", "1.1.0-rc1", "1.1.0", "1.2.0.RELEASE", "1.3.0-SNAPSHOT", "1.3.0", "2.0.0", "2.1.0")
        assertEquals(listOf("1.0.0", "1.1.0", "1.2.0.RELEASE", "1.3.0", "2.0.0"), VersionBisect.candidates(released, "1.0.0", "2.0.0"))
    }
}
