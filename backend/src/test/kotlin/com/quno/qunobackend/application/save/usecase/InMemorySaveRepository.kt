package com.quno.qunobackend.application.save.usecase

import com.quno.qunobackend.domain.save.SaveRepository

class InMemorySaveRepository : SaveRepository {
    private val saves = mutableSetOf<Pair<Long, Long>>()

    override fun save(userId: Long, questionId: Long) {
        saves += userId to questionId
    }

    override fun unsave(userId: Long, questionId: Long) {
        saves -= userId to questionId
    }

    override fun isSaved(userId: Long, questionId: Long): Boolean = (userId to questionId) in saves

    override fun findSavedQuestionIds(userId: Long): List<Long> =
        saves.filter { it.first == userId }.map { it.second }
}
