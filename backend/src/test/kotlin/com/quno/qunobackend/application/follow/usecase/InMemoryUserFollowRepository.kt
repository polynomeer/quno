package com.quno.qunobackend.application.follow.usecase

import com.quno.qunobackend.domain.follow.UserFollowRepository

class InMemoryUserFollowRepository : UserFollowRepository {
    private val follows = mutableSetOf<Pair<Long, Long>>()

    override fun follow(followerId: Long, followeeId: Long) {
        follows += followerId to followeeId
    }

    override fun unfollow(followerId: Long, followeeId: Long) {
        follows -= followerId to followeeId
    }

    override fun isFollowing(followerId: Long, followeeId: Long): Boolean = (followerId to followeeId) in follows

    override fun findFolloweeIds(followerId: Long): List<Long> =
        follows.filter { it.first == followerId }.map { it.second }
}
