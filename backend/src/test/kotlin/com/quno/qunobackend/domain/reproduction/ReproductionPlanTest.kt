package com.quno.qunobackend.domain.reproduction

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.Duration
import java.time.Instant
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ReproductionPlanTest {
    private fun capsule(matrixPackage: String? = "com.zaxxer:HikariCP", versions: List<String> = emptyList()) = ReproductionCapsule.attach(1L, "octo/demo", "main", ReproductionKind.GRADLE, "test", 9L, matrixPackage, versions)

    @Test
    fun `the matrix takes the author's versions, the question's, then the answers' edges, in version order`() {
        assertEquals(
            listOf("4.0.3", "5.0.0", "5.1.0", "6.0.0"),
            ReproductionMatrix.plan(capsule(versions = listOf("4.0.3")), "^5.1.0", listOf("5.0.0", "6.0.0", "5.1")),
        )
        assertEquals(listOf(null), ReproductionMatrix.plan(capsule(matrixPackage = null), "5.1.0", listOf("6.0.0")), "no package, one cell")
        assertEquals(listOf(null), ReproductionMatrix.plan(capsule(), null, emptyList()), "a package with nothing known still runs once")
    }

    @Test
    fun `the matrix stops at five versions, keeping the most specific sources`() {
        val plan = ReproductionMatrix.plan(capsule(versions = listOf("1.0", "2.0", "3.0", "4.0")), "5.0", listOf("6.0", "7.0"))
        assertEquals(listOf("1.0", "2.0", "3.0", "4.0", "5.0"), plan)
    }

    @Test
    fun `matrix versions need a package and must be versions`() {
        assertThrows<InvalidReproductionCapsuleException> { capsule(matrixPackage = null, versions = listOf("1.0")) }
        assertThrows<InvalidReproductionCapsuleException> { capsule(versions = listOf("latest")) }
        assertThrows<InvalidReproductionCapsuleException> { capsule(versions = (1..6).map { "$it.0" }) }
        assertThrows<InvalidReproductionCapsuleException> { capsule(matrixPackage = "two words") }
    }

    @Test
    fun `scheduled runs wait for an interval that shrinks with views`() {
        val now = Instant.parse("2026-11-02T00:00:00Z")
        assertEquals("manual", ReproductionSchedule.dueReason("workflow_dispatch", false, now, 0, now))
        assertEquals("rerun-requested", ReproductionSchedule.dueReason("schedule", true, now, 0, now))
        assertEquals("first-run", ReproductionSchedule.dueReason("schedule", false, null, 0, now))
        assertNull(ReproductionSchedule.dueReason("schedule", false, now.minus(Duration.ofDays(6)), 5, now))
        assertEquals("interval", ReproductionSchedule.dueReason("schedule", false, now.minus(Duration.ofDays(7)), 5, now))
        assertEquals("interval", ReproductionSchedule.dueReason("schedule", false, now.minus(Duration.ofDays(1)), 150, now))
        assertNull(ReproductionSchedule.dueReason("schedule", false, now.minus(Duration.ofDays(2)), 50, now))
    }

    @Test
    fun `the latest release always gets a cell, and an untested one is due at once`() {
        val full = ReproductionMatrix.plan(capsule(versions = listOf("1.0", "2.0", "3.0", "4.0", "5.0")), null, emptyList(), latestRelease = "9.0")
        assertEquals(listOf("1.0", "2.0", "3.0", "4.0", "9.0"), full)
        val now = Instant.parse("2026-11-02T00:00:00Z")
        assertEquals("new-release", ReproductionSchedule.dueReason("schedule", false, now, 0, now, releaseUntested = true))
        assertEquals("first-run", ReproductionSchedule.dueReason("schedule", false, null, 0, now, releaseUntested = true))
    }

    private fun run(version: String, result: ReproductionResult, environment: String = "ubuntu24", trusted: Boolean = true, runner: String = "github-hosted") = ReproductionRun(
        id = 1,
        capsuleId = 1,
        result = result,
        exitCode = 0,
        commitSha = "c",
        environment = environment,
        runnerEnvironment = runner,
        trustedWorkflow = trusted,
        workflowRef = "w",
        runId = 1,
        runAttempt = 1,
        runUrl = "u",
        durationMs = 1,
        logTail = "l",
        maskedSecretCount = 0,
        finishedAt = Instant.now(),
        dependencyVersion = version,
    )

    private fun detect(result: ReproductionResult, cells: List<ReproductionRun>, version: String = "6.0.0", trusted: Boolean = true, runner: String = "github-hosted") = ReleaseRegression.detect(result, trusted, runner, "ubuntu24", version, "6.0.0", cells)

    @Test
    fun `a release that fails where the version before passed broke, and the reverse is a candidate fix`() {
        val passedBefore = listOf(run("4.0.0", ReproductionResult.REPRODUCED), run("5.1.0", ReproductionResult.NOT_REPRODUCED))
        assertEquals(ReproductionTransition.BROKE, detect(ReproductionResult.REPRODUCED, passedBefore)?.transition)
        assertEquals("5.1.0", detect(ReproductionResult.REPRODUCED, passedBefore)?.comparedRun?.dependencyVersion, "the nearest version below")
        assertEquals(ReproductionTransition.FIXED, detect(ReproductionResult.NOT_REPRODUCED, listOf(run("5.1.0", ReproductionResult.REPRODUCED)))?.transition)
        assertNull(detect(ReproductionResult.NOT_REPRODUCED, passedBefore), "no change")
    }

    @Test
    fun `only trusted conclusive runs of the release compare, once per cell and environment`() {
        val before = listOf(run("5.1.0", ReproductionResult.NOT_REPRODUCED))
        assertNull(detect(ReproductionResult.BUILD_FAILED, before))
        assertNull(detect(ReproductionResult.REPRODUCED, before, trusted = false))
        assertNull(detect(ReproductionResult.REPRODUCED, before, runner = "self-hosted"))
        assertNull(detect(ReproductionResult.REPRODUCED, before, version = "5.2.0"), "not the release")
        assertNull(detect(ReproductionResult.REPRODUCED, emptyList()), "nothing to compare with")
        assertNull(detect(ReproductionResult.REPRODUCED, listOf(run("5.1.0", ReproductionResult.NOT_REPRODUCED, environment = "macos15"))), "another environment says nothing")
        assertNull(detect(ReproductionResult.REPRODUCED, listOf(run("5.1.0", ReproductionResult.NOT_REPRODUCED, trusted = false))), "a hand-written baseline says nothing")
        assertNull(detect(ReproductionResult.REPRODUCED, before + run("6.0.0", ReproductionResult.REPRODUCED)), "already reported for this cell")
    }

    @Test
    fun `a run version must fit the capsule`() {
        assertThrows<ReproductionRunMismatchException> { capsule(matrixPackage = null).checkCell("1.0") }
        assertThrows<ReproductionRunMismatchException> { capsule().checkCell("latest") }
        capsule().checkCell("5.1.0")
        capsule(matrixPackage = null).checkCell(null)
    }
}
