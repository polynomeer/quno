package com.quno.qunobackend.application.snapshot

import com.quno.qunobackend.domain.snapshot.EnvironmentSnapshot
import com.quno.qunobackend.domain.snapshot.EnvironmentSnapshotRepository
import com.quno.qunobackend.domain.snapshot.SnapshotNormalizer
import org.springframework.stereotype.Component
import java.time.Instant

/** Shared by create and revise (Phase 93, ADR-0083): normalise, then keep it as the question's latest snapshot. */
@Component
class EnvironmentSnapshotAttacher(
    private val normalizer: SnapshotNormalizer,
    private val repository: EnvironmentSnapshotRepository,
) {
    /** Throws InvalidEnvironmentSnapshotException before anything is stored; the caller's transaction then rolls back. */
    fun attach(questionId: Long, versionNumber: Int, rawJson: String) {
        val normalized = normalizer.normalize(rawJson)
        repository.replaceForQuestion(
            EnvironmentSnapshot(
                questionId = questionId,
                versionNumber = versionNumber,
                schemaVersion = normalized.schemaVersion,
                source = normalized.source,
                collector = normalized.collector,
                fingerprint = normalized.fingerprint,
                serverRedactions = normalized.serverRedactions,
                document = normalized.document,
                createdAt = Instant.now(),
            ),
        )
    }
}
