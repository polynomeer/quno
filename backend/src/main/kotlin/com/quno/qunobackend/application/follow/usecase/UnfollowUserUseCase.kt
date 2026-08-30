package com.quno.qunobackend.application.follow.usecase

import com.quno.qunobackend.domain.follow.UserFollowRepository
import org.springframework.stereotype.Service

@Service
class UnfollowUserUseCase(
    private val userFollowRepository: UserFollowRepository,
) {
    fun execute(followerId: Long, followeeId: Long) {
        userFollowRepository.unfollow(followerId, followeeId)
    }
}
