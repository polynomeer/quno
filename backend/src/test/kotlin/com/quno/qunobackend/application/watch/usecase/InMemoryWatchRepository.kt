package com.quno.qunobackend.application.watch.usecase

import com.quno.qunobackend.domain.watch.WatchRepository

class InMemoryWatchRepository : WatchRepository {
    private val watches = mutableSetOf<Pair<Long, Long>>()

    override fun watch(userId: Long, questionId: Long) {
        watches += userId to questionId
    }

    override fun unwatch(userId: Long, questionId: Long) {
        watches -= userId to questionId
    }

    override fun isWatching(userId: Long, questionId: Long): Boolean = (userId to questionId) in watches

    override fun findWatchedQuestionIds(userId: Long): List<Long> =
        watches.filter { it.first == userId }.map { it.second }

    override fun findWatcherIds(questionId: Long): List<Long> =
        watches.filter { it.second == questionId }.map { it.first }
}
