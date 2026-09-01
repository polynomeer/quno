package com.quno.qunobackend.application.livechat.usecase

import com.quno.qunobackend.domain.livechat.LiveChatPresenceTracker

class FakeLiveChatPresenceTracker : LiveChatPresenceTracker {
    private val viewers = mutableMapOf<Long, MutableSet<String>>()

    override fun join(questionId: Long, sessionId: String) {
        viewers.getOrPut(questionId) { mutableSetOf() }.add(sessionId)
    }

    override fun leave(questionId: Long, sessionId: String) {
        viewers[questionId]?.remove(sessionId)
    }

    override fun countViewers(questionId: Long): Long = (viewers[questionId]?.size ?: 0).toLong()
}
