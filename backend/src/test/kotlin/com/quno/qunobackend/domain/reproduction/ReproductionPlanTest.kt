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
    fun `a run version must fit the capsule`() {
        assertThrows<ReproductionRunMismatchException> { capsule(matrixPackage = null).checkCell("1.0") }
        assertThrows<ReproductionRunMismatchException> { capsule().checkCell("latest") }
        capsule().checkCell("5.1.0")
        capsule(matrixPackage = null).checkCell(null)
    }
}
