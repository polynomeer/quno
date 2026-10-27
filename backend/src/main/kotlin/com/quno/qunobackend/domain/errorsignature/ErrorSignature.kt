package com.quno.qunobackend.domain.errorsignature

/**
 * One exception chain reduced to what stays the same across machines (Phase 64, ADR-0075).
 *
 * [signatureHash] keys on the root cause's class plus its top frames — the strong match. It is
 * null when the text has no stack frames. [messageKey] keys on the class plus the normalised
 * message — the weaker fallback. Both are the first 16 hex chars of a SHA-256, the same keys the
 * Phase 91 experiment (experiments/phase-91-mcp/src/signature.js) produces.
 */
data class ErrorSignature(
    val exceptionClass: String,
    val normalizedMessage: String,
    val frames: List<String>,
    val signatureHash: String?,
    val messageKey: String,
    /** Every exception in the chain, outermost first; the last one is the root cause. */
    val chain: List<String>,
)
