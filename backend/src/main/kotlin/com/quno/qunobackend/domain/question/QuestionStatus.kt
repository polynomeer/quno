package com.quno.qunobackend.domain.question

/**
 * A subset of the full target model in docs/product/vision.md#질문-상태-모델 — no READY_FOR_REVIEW,
 * IN_REVIEW, ANSWERED, DUPLICATED, MERGED, or REOPENED yet. OUTDATED (Phase 8.1) is reached only
 * by explicit user marking, never automatic technology-version detection (see ADR-0017); a
 * question leaves it the same way it leaves NEEDS_INFO — any revise() call.
 */
enum class QuestionStatus {
    OPEN,
    NEEDS_INFO,
    UPDATED,
    RESOLVED,
    OUTDATED,
}
