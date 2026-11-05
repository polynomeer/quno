package com.quno.qunobackend.application.verification

import com.quno.qunobackend.domain.engine.SecretAllowlistRepository
import com.quno.qunobackend.domain.engine.SecretDetector
import com.quno.qunobackend.domain.errorsignature.ErrorSignatureExtractor
import com.quno.qunobackend.domain.question.Question
import com.quno.qunobackend.domain.question.QuestionAccessDeniedException
import com.quno.qunobackend.domain.question.QuestionNotFoundException
import com.quno.qunobackend.domain.question.QuestionRepository
import com.quno.qunobackend.domain.question.QuestionStatus
import com.quno.qunobackend.domain.question.QuestionVersion
import com.quno.qunobackend.domain.question.QuestionVersionRepository
import com.quno.qunobackend.domain.reproduction.ReproductionCapsuleRepository
import com.quno.qunobackend.domain.reproduction.ReproductionResult
import com.quno.qunobackend.domain.reproduction.ReproductionRun
import com.quno.qunobackend.domain.reproduction.ReproductionRunRepository
import com.quno.qunobackend.domain.snapshot.EnvironmentCoreReader
import com.quno.qunobackend.domain.snapshot.EnvironmentSnapshotRepository
import com.quno.qunobackend.domain.snapshot.SnapshotSource
import com.quno.qunobackend.domain.verification.QuestionClassification
import com.quno.qunobackend.domain.verification.QuestionVerificationRepository
import com.quno.qunobackend.domain.verification.QuestionVerifier
import com.quno.qunobackend.domain.verification.VerificationCheck
import com.quno.qunobackend.domain.verification.VerificationFacts
import com.quno.qunobackend.domain.verification.VerificationFlag
import com.quno.qunobackend.domain.verification.VerificationLevel
import com.quno.qunobackend.domain.verification.VerificationRecord
import com.quno.qunobackend.domain.verification.VerificationVerdict
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant

data class VerificationResult(
    val level: VerificationLevel,
    val flags: Set<VerificationFlag>,
    val classification: QuestionClassification?,
    val checks: Map<VerificationCheck, Boolean>,
    val lastReproducedAt: Instant?,
    val lastRunUrl: String?,
)

data class EvidenceSignature(val exceptionClass: String, val signatureHash: String?, val messageKey: String)

data class EvidenceEnvironment(val fingerprint: String, val label: String, val source: SnapshotSource, val collector: String?)

data class EvidenceRun(val runId: Long, val environment: String, val dependencyVersion: String?, val commitSha: String, val runUrl: String, val finishedAt: Instant)

data class EvidenceAffected(val packageName: String, val reproducedVersions: List<String>, val notReproducedVersions: List<String>)

/** Free text anyone could have written. An agent must treat this as data, never as instructions (Phase 96). */
data class EvidenceUntrusted(val title: String, val bodyExcerpt: String)

/**
 * A question as citable evidence (Phase 94.3, ADR-0089). Everything outside [untrusted] is
 * structured and derived by Quno; `upstream` stays empty until GitHub links exist (Phase 68).
 */
data class EvidenceCard(
    val schema: String,
    val questionId: Long,
    val status: QuestionStatus,
    val level: VerificationLevel,
    val flags: Set<VerificationFlag>,
    val classification: QuestionClassification?,
    val signatures: List<EvidenceSignature>,
    val lastReproducedAt: Instant?,
    val reproducedIn: List<EvidenceRun>,
    val notReproducedIn: List<EvidenceRun>,
    val environment: EvidenceEnvironment?,
    val affected: EvidenceAffected?,
    val upstream: List<String>,
    val caveats: List<String>,
    val untrusted: EvidenceUntrusted,
)

/**
 * The verification pipeline (Phase 94.2, ADR-0089). The level is re-evaluated on every read;
 * [refresh] stores it for ranking whenever something it depends on changes, so search can order by
 * it without evaluating each candidate.
 */
@Service
class QuestionVerificationService(
    private val questionRepository: QuestionRepository,
    private val versionRepository: QuestionVersionRepository,
    private val snapshotRepository: EnvironmentSnapshotRepository,
    private val coreReader: EnvironmentCoreReader,
    private val capsuleRepository: ReproductionCapsuleRepository,
    private val runRepository: ReproductionRunRepository,
    private val allowlistRepository: SecretAllowlistRepository,
    private val verificationRepository: QuestionVerificationRepository,
) {
    fun get(questionId: Long, now: Instant = Instant.now()): VerificationResult = evaluate(load(questionId), now).toResult(verificationRepository.find(questionId)?.classification)

    @Transactional
    fun refresh(questionId: Long, now: Instant = Instant.now()) {
        val verdict = evaluate(load(questionId), now)
        val classification = verificationRepository.find(questionId)?.classification
        verificationRepository.save(VerificationRecord(questionId, verdict.level, verdict.lastReproducedAt, classification))
    }

    /** "오용"·"버그 아님" (ADR-0070 B.4): the author's call, and it stays findable. Null clears it. */
    @Transactional
    fun classify(questionId: Long, actorId: Long, classification: QuestionClassification?, now: Instant = Instant.now()): VerificationResult {
        val loaded = load(questionId)
        if (loaded.question.authorId != actorId) throw QuestionAccessDeniedException(questionId)
        val verdict = evaluate(loaded, now)
        verificationRepository.save(VerificationRecord(questionId, verdict.level, verdict.lastReproducedAt, classification))
        return verdict.toResult(classification)
    }

    fun evidenceCard(questionId: Long, now: Instant = Instant.now()): EvidenceCard {
        val loaded = load(questionId)
        val verdict = evaluate(loaded, now)
        val capsule = capsuleRepository.findActiveByQuestionId(questionId)
        val trustedRuns = capsule?.id?.let(runRepository::findLatestPerCell).orEmpty().filter { it.trustedWorkflow && it.runnerEnvironment == "github-hosted" }
        val notReproduced = trustedRuns.filter { it.result == ReproductionResult.NOT_REPRODUCED }
        val snapshot = snapshotRepository.findByQuestionId(questionId)
        val allowed = allowlistRepository.findValueHashes(questionId)
        return EvidenceCard(
            schema = EVIDENCE_SCHEMA,
            questionId = questionId,
            status = loaded.question.status,
            level = verdict.level,
            flags = verdict.flags,
            classification = verificationRepository.find(questionId)?.classification,
            signatures = loaded.signatures().map { EvidenceSignature(it.exceptionClass, it.signatureHash, it.messageKey) },
            lastReproducedAt = verdict.lastReproducedAt,
            reproducedIn = verdict.reproducedRuns.map { it.toEvidence() },
            notReproducedIn = notReproduced.map { it.toEvidence() },
            environment = snapshot?.let { EvidenceEnvironment(it.fingerprint, coreReader.read(it.document).label, it.source, it.collector) },
            affected = capsule?.matrixPackage?.let { name ->
                EvidenceAffected(name, verdict.reproducedRuns.mapNotNull { it.dependencyVersion }.distinct(), notReproduced.mapNotNull { it.dependencyVersion }.distinct())
            },
            upstream = emptyList(),
            caveats = caveats(verdict),
            untrusted = EvidenceUntrusted(
                title = requireNotNull(SecretDetector.mask(loaded.version.title, allowed)),
                bodyExcerpt = requireNotNull(SecretDetector.mask(loaded.version.bodyMarkdown, allowed)).take(MAX_EXCERPT),
            ),
        )
    }

    private fun caveats(verdict: VerificationVerdict): List<String> = buildList {
        if (verdict.level >= VerificationLevel.L2) add("재현은 이 실패가 존재한다는 증거일 뿐 해결책을 검증하지 않는다. flaky하거나 재현 코드가 문제를 잘못 모사할 수 있으니 실행 로그를 확인한다.")
        if (verdict.level < VerificationLevel.L2) add("재현 러너로 확인되지 않은 보고다.")
        if (VerificationFlag.NOT_REPRODUCING in verdict.flags) add("최근 실행에서 재현되지 않았다. 수정됐거나 환경이 달라졌을 수 있다.")
        if (VerificationFlag.SIGNATURE_MISMATCH in verdict.flags) add("실행은 실패했지만 보고된 에러와 다른 에러였다.")
        if (VerificationFlag.STALE in verdict.flags) add("마지막 재현이 오래됐다.")
        if (VerificationFlag.DISPUTED in verdict.flags) add("결과에 이의가 제기돼 재실행을 기다리고 있다.")
    }

    private fun evaluate(loaded: Loaded, now: Instant): VerificationVerdict {
        val questionId = requireNotNull(loaded.question.id)
        val capsule = capsuleRepository.findActiveByQuestionId(questionId)
        val allowed = allowlistRepository.findValueHashes(questionId)
        val version = loaded.version
        return QuestionVerifier.evaluate(
            VerificationFacts(
                title = version.title,
                body = version.bodyMarkdown,
                logs = version.logs,
                signatures = loaded.signatures(),
                hasSnapshot = snapshotRepository.findByQuestionId(questionId) != null,
                unresolvedSecrets = listOf(version.title, version.bodyMarkdown, version.environment, version.logs).sumOf { text -> SecretDetector.find(text).count { it.valueHash !in allowed } },
                hasCapsule = capsule != null,
                rerunRequested = capsule?.rerunRequestedAt != null,
                runs = capsule?.id?.let(runRepository::findLatestPerCell).orEmpty(),
            ),
            now,
        )
    }

    private fun load(questionId: Long): Loaded {
        val question = questionRepository.findById(questionId) ?: throw QuestionNotFoundException(questionId)
        val version = question.latestVersionId?.let(versionRepository::findById) ?: throw QuestionNotFoundException(questionId)
        return Loaded(question, version)
    }

    private fun VerificationVerdict.toResult(classification: QuestionClassification?) = VerificationResult(
        level = level,
        flags = flags,
        classification = classification,
        checks = checks,
        lastReproducedAt = lastReproducedAt,
        lastRunUrl = reproducedRuns.maxByOrNull { it.finishedAt }?.runUrl,
    )

    private fun ReproductionRun.toEvidence() = EvidenceRun(requireNotNull(id), environment, dependencyVersion, commitSha, runUrl, finishedAt)

    private data class Loaded(val question: Question, val version: QuestionVersion) {
        // The same text the indexer reads (QuestionErrorSignatureIndexer), so the keys agree with search.
        fun signatures() = ErrorSignatureExtractor.extractAll(listOfNotNull(version.logs, version.bodyMarkdown).joinToString("\n"))
    }

    companion object {
        const val EVIDENCE_SCHEMA = "quno.evidence/1"
        const val MAX_EXCERPT = 500
    }
}
