package com.quno.qunobackend.domain.livechat

/**
 * Tracks who currently has a question's detail page open — the "현재 17명이 이 질문을 보고
 * 있습니다" signal from the original brainstorm (Phase 24, ADR-0036). Implemented over Redis
 * (infrastructure/livechat/RedisLiveChatPresenceTracker) since it's ephemeral, per-instance-safe
 * membership tracking, not a durable record.
 */
interface LiveChatPresenceTracker {
    /** Idempotent — re-joining with the same sessionId doesn't double-count. */
    fun join(questionId: Long, sessionId: String)
    fun leave(questionId: Long, sessionId: String)
    fun countViewers(questionId: Long): Long
}
