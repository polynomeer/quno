package com.quno.qunobackend.domain.reproduction

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ReproductionCapsuleTest {
    private fun attach(repository: String = "octo/demo", ref: String = "main", kind: ReproductionKind = ReproductionKind.SCRIPT, command: String = "./repro.sh") = ReproductionCapsule.attach(1L, repository, ref, kind, command, 9L)

    @Test
    fun `exit codes follow git bisect run`() {
        assertEquals(ReproductionResult.NOT_REPRODUCED, ReproductionResult.of(false, 0))
        assertEquals(ReproductionResult.REPRODUCED, ReproductionResult.of(false, 1))
        assertEquals(ReproductionResult.REPRODUCED, ReproductionResult.of(false, 127))
        assertEquals(ReproductionResult.INCONCLUSIVE, ReproductionResult.of(false, 125))
        assertEquals(ReproductionResult.INCONCLUSIVE, ReproductionResult.of(false, 137), "killed (OOM, timeout) proves nothing")
        assertEquals(ReproductionResult.INCONCLUSIVE, ReproductionResult.of(false, null))
        assertEquals(ReproductionResult.BUILD_FAILED, ReproductionResult.of(true, 1))
    }

    @Test
    fun `repositories are owner and name on github, given either way`() {
        assertEquals("octo/demo", attach(repository = "https://github.com/octo/demo.git").repository)
        assertEquals("octo/demo", attach(repository = "github.com/octo/demo/").repository)
        listOf("octo", "octo/demo/tree/main", "https://gitlab.com/octo/demo", "-x/demo").forEach {
            assertThrows<InvalidReproductionCapsuleException>(it) { attach(repository = it) }
        }
    }

    @Test
    fun `refs, commands and Dockerfile paths are checked`() {
        assertThrows<InvalidReproductionCapsuleException> { attach(ref = "main; rm -rf /") }
        assertThrows<InvalidReproductionCapsuleException> { attach(ref = "../etc") }
        assertThrows<InvalidReproductionCapsuleException> { attach(command = " ") }
        assertThrows<InvalidReproductionCapsuleException> { attach(kind = ReproductionKind.DOCKERFILE, command = "../Dockerfile") }
        assertEquals("deadbeef".repeat(5), attach(ref = "DEADBEEF".repeat(5)).pinnedSha)
        assertNull(attach(ref = "v1.2.0").pinnedSha)
    }

    @Test
    fun `one outstanding rerun request, answered by the next run`() {
        val capsule = ReproductionCapsule.reconstitute(5L, 1L, "octo/demo", "main", ReproductionKind.SCRIPT, "./repro.sh", 9L, java.time.Instant.now(), null, null, null, null)
        val requested = capsule.requestRerun(3L, " 안 돼요 ")
        assertEquals("안 돼요", requested.rerunReason)
        assertThrows<RerunAlreadyRequestedException> { requested.requestRerun(4L, "again") }
        assertNull(requested.rerunAnswered().rerunRequestedAt)
        assertTrue(capsule.matches(ReproductionKind.SCRIPT, " ./repro.sh "))
        assertTrue(capsule.isFrom("Octo/Demo"))
    }
}
