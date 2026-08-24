package com.quno.qunobackend.application.tag.usecase

import com.quno.qunobackend.domain.tag.UserTagFollowRepository
import org.springframework.stereotype.Service

@Service
class UnfollowTagUseCase(
    private val userTagFollowRepository: UserTagFollowRepository,
) {
    fun execute(userId: Long, tagId: Long) {
        userTagFollowRepository.unfollow(userId, tagId)
    }
}
