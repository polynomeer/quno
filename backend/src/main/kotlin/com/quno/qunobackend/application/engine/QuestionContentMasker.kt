package com.quno.qunobackend.application.engine

import com.quno.qunobackend.domain.engine.SecretAllowlistRepository
import com.quno.qunobackend.domain.engine.SecretDetector
import org.springframework.stereotype.Component

/**
 * Read-time masking (Phase 78.4, ADR-0081): everyone but the question's author gets secrets
 * replaced. Computed on every read, so a rule change reaches old versions without a migration.
 */
@Component
class QuestionContentMasker(
    private val allowlistRepository: SecretAllowlistRepository,
) {
    /** A function that masks text of [questionId] for [viewerId]; the identity for the author. */
    fun forViewer(questionId: Long, authorId: Long, viewerId: Long?): (String?) -> String? {
        if (viewerId != null && viewerId == authorId) return { it }
        val allowed = allowlistRepository.findValueHashes(questionId)
        return { SecretDetector.mask(it, allowed) }
    }

    fun maskedCount(questionId: Long, authorId: Long, viewerId: Long?, vararg texts: String?): Int {
        if (viewerId != null && viewerId == authorId) return 0
        val allowed = allowlistRepository.findValueHashes(questionId)
        return texts.sumOf { text -> SecretDetector.find(text).count { it.valueHash !in allowed } }
    }
}
