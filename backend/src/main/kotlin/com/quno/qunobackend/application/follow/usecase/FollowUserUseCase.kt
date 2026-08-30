package com.quno.qunobackend.application.follow.usecase

import com.quno.qunobackend.domain.follow.SelfFollowException
import com.quno.qunobackend.domain.follow.UserFollowRepository
import com.quno.qunobackend.domain.user.UserNotFoundException
import com.quno.qunobackend.domain.user.UserRepository
import org.springframework.stereotype.Service

@Service
class FollowUserUseCase(
    private val userRepository: UserRepository,
    private val userFollowRepository: UserFollowRepository,
) {
    fun execute(followerId: Long, followeeId: Long) {
        if (followerId == followeeId) throw SelfFollowException(followerId)
        userRepository.findById(followeeId) ?: throw UserNotFoundException(followeeId)
        userFollowRepository.follow(followerId, followeeId)
    }
}
