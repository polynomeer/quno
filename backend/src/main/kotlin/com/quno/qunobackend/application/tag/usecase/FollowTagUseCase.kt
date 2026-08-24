package com.quno.qunobackend.application.tag.usecase

import com.quno.qunobackend.domain.tag.TagNotFoundException
import com.quno.qunobackend.domain.tag.TagRepository
import com.quno.qunobackend.domain.tag.UserTagFollowRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class FollowTagUseCase(
    private val tagRepository: TagRepository,
    private val userTagFollowRepository: UserTagFollowRepository,
) {
    @Transactional
    fun execute(userId: Long, tagId: Long) {
        tagRepository.findById(tagId) ?: throw TagNotFoundException(tagId)
        userTagFollowRepository.follow(userId, tagId)
    }
}
