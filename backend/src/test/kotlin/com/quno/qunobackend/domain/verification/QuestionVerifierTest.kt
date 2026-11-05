package com.quno.qunobackend.domain.verification

import com.quno.qunobackend.domain.errorsignature.ErrorSignatureExtractor
import com.quno.qunobackend.domain.reproduction.ReproductionResult
import com.quno.qunobackend.domain.reproduction.ReproductionRun
import org.junit.jupiter.api.Test
import java.time.Duration
import java.time.Instant
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class QuestionVerifierTest {
    private val now = Instant.parse("2026-11-04T00:00:00Z")
    private val trace = "java.lang.IllegalStateException: pool exhausted\n\tat com.zaxxer.hikari.Pool.get(Pool.java:42)"
    private val reported = ErrorSignatureExtractor.extractAll(trace)

    private fun run(result: ReproductionResult = ReproductionResult.REPRODUCED, log: String = trace, trusted: Boolean = true, runner: String = "github-hosted", at: Instant = now): ReproductionRun {
        val signatures = ErrorSignatureExtractor.extractAll(log)
        return ReproductionRun(
            id = 1, capsuleId = 1, result = result, exitCode = 1, commitSha = "c".repeat(40), environment = "ubuntu24", runnerEnvironment = runner, trustedWorkflow = trusted,
            workflowRef = "w", runId = 1, runAttempt = 1, runUrl = "u", durationMs = 1, logTail = log, maskedSecretCount = 0, finishedAt = at,
            signatureHashes = signatures.mapNotNull { it.signatureHash }, messageKeys = signatures.map { it.messageKey },
        )
    }

    private fun facts(
        body: String = "Expected: return. Actual: leak.\n1. run it",
        signatures: List<com.quno.qunobackend.domain.errorsignature.ErrorSignature> = reported,
        hasSnapshot: Boolean = true,
        secrets: Int = 0,
        hasCapsule: Boolean = true,
        rerunRequested: Boolean = false,
        runs: List<ReproductionRun> = emptyList(),
    ) = VerificationFacts("t", body, trace, signatures, hasSnapshot, secrets, hasCapsule, rerunRequested, runs)

    private fun verdict(facts: VerificationFacts) = QuestionVerifier.evaluate(facts, now)

    @Test
    fun `L1 needs every structural check`() {
        assertEquals(VerificationLevel.L1, verdict(facts()).level)
        listOf(
            facts(signatures = emptyList()),
            facts(hasSnapshot = false),
            facts(body = "It leaks.\n1. run it"),
            facts(body = "Expected a thing, actual other", hasCapsule = false),
            facts(secrets = 1),
        ).forEach { assertEquals(VerificationLevel.L0, verdict(it).level, it.toString()) }
    }

    @Test
    fun `Korean wording and fenced code count as structure`() {
        assertEquals(VerificationLevel.L1, verdict(facts(body = "기대: 반환. 실제: 누수.\n```\ncode\n```", hasCapsule = false)).level)
    }

    @Test
    fun `L2 needs a trusted hosted run failing with the reported error`() {
        assertEquals(VerificationLevel.L2, verdict(facts(runs = listOf(run()))).level)
        assertEquals(VerificationLevel.L1, verdict(facts(runs = listOf(run(trusted = false)))).level)
        assertEquals(VerificationLevel.L1, verdict(facts(runs = listOf(run(runner = "self-hosted")))).level)
        assertEquals(VerificationLevel.L1, verdict(facts(runs = listOf(run(result = ReproductionResult.BUILD_FAILED)))).level)
    }

    @Test
    fun `a different error is a mismatch, a matching message with other frames is not`() {
        val other = verdict(facts(runs = listOf(run(log = "java.lang.NullPointerException: x\n\tat a.B.c(B.java:1)"))))
        assertEquals(VerificationLevel.L1, other.level)
        assertEquals(setOf(VerificationFlag.SIGNATURE_MISMATCH), other.flags)
        assertEquals(VerificationLevel.L2, verdict(facts(runs = listOf(run(log = "java.lang.IllegalStateException: pool exhausted\n\tat demo.Repro.main(Repro.java:3)")))).level)
    }

    @Test
    fun `L2 does not outrank missing structure`() {
        assertEquals(VerificationLevel.L0, verdict(facts(hasSnapshot = false, runs = listOf(run()))).level)
    }

    @Test
    fun `a passing latest run says not reproducing`() {
        val passing = verdict(facts(runs = listOf(run(result = ReproductionResult.NOT_REPRODUCED, log = "ok"))))
        assertEquals(VerificationLevel.L1, passing.level)
        assertEquals(setOf(VerificationFlag.NOT_REPRODUCING), passing.flags)
        val oneCellStillFails = verdict(facts(runs = listOf(run(), run(result = ReproductionResult.NOT_REPRODUCED, log = "ok"))))
        assertEquals(VerificationLevel.L2, oneCellStillFails.level)
        assertFalse(VerificationFlag.NOT_REPRODUCING in oneCellStillFails.flags)
    }

    @Test
    fun `an old reproduction stays L2 but is stale, and a dispute shows`() {
        val old = verdict(facts(runs = listOf(run(at = now.minus(QuestionVerifier.FRESH_FOR).minus(Duration.ofDays(1)))), rerunRequested = true))
        assertEquals(VerificationLevel.L2, old.level)
        assertTrue(VerificationFlag.STALE in old.flags)
        assertTrue(VerificationFlag.DISPUTED in old.flags)
    }
}
