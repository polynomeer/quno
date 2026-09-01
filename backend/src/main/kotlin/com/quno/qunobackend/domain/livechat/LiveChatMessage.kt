package com.quno.qunobackend.domain.livechat

import java.time.Instant

/**
 * A single chat message (Phase 24, ADR-0036) — the first real MongoDB usage in this codebase
 * (previously declared in architecture docs but never implemented). High-volume, append-only,
 * no relational invariants worth enforcing across rows — exactly the "구조가 자주 바뀌거나
 * 대량 쓰기가 발생하는" shape docs/architecture/domain-model.md's MongoDB section describes,
 * and separating it keeps this write traffic off the core Postgres tables.
 */
class LiveChatMessage private constructor(
    val id: String?,
    val roomId: Long,
    val senderId: Long,
    val body: String,
    val createdAt: Instant,
) {
    companion object {
        fun post(roomId: Long, senderId: Long, body: String): LiveChatMessage {
            val trimmed = body.trim()
            require(trimmed.isNotBlank()) { "body must not be blank" }
            require(trimmed.length <= 2000) { "body must be at most 2000 characters" }
            return LiveChatMessage(id = null, roomId = roomId, senderId = senderId, body = trimmed, createdAt = Instant.now())
        }

        fun reconstitute(id: String, roomId: Long, senderId: Long, body: String, createdAt: Instant): LiveChatMessage =
            LiveChatMessage(id, roomId, senderId, body, createdAt)
    }
}
