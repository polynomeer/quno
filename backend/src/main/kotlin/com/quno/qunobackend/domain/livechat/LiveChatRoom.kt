package com.quno.qunobackend.domain.livechat

import java.time.Instant

/**
 * A question's real-time discussion space (Phase 24, ADR-0036, mvp-scope.md 로드맵 Phase 6 —
 * see docs/archive 19장). At most one room per question — "필요한 경우 즉시 Live Chat을
 * 생성" means opening finds-or-creates rather than allowing multiple concurrent rooms per
 * question. There is no close/archive: the room and its message history simply persist
 * alongside the question, even after it's RESOLVED.
 */
class LiveChatRoom private constructor(
    val id: Long?,
    val questionId: Long,
    val createdBy: Long,
    val createdAt: Instant,
) {
    companion object {
        fun open(questionId: Long, createdBy: Long): LiveChatRoom =
            LiveChatRoom(id = null, questionId = questionId, createdBy = createdBy, createdAt = Instant.now())

        fun reconstitute(id: Long, questionId: Long, createdBy: Long, createdAt: Instant): LiveChatRoom =
            LiveChatRoom(id, questionId, createdBy, createdAt)
    }
}
