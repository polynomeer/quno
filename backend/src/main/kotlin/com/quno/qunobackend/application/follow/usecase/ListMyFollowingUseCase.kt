package com.quno.qunobackend.application.follow.usecase

import com.quno.qunobackend.application.follow.dto.FolloweeResult
import com.quno.qunobackend.domain.follow.UserFollowRepository
import com.quno.qunobackend.domain.user.UserRepository
import org.springframework.stereotype.Service

@Service
class ListMyFollowingUseCase(
    private val userFollowRepository: UserFollowRepository,
    private val userRepository: UserRepository,
) {
    fun execute(followerId: Long): List<FolloweeResult> =
        userFollowRepository.findFolloweeIds(followerId).mapNotNull { followeeId ->
            userRepository.findById(followeeId)?.let { user ->
                FolloweeResult(userId = followeeId, nickname = user.nickname)
            }
        }
}
