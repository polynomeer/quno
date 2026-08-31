package com.quno.qunobackend.application.user.usecase

import com.quno.qunobackend.application.user.dto.MyProfileResult
import com.quno.qunobackend.domain.user.UserNotFoundException
import com.quno.qunobackend.domain.user.UserRepository
import org.springframework.stereotype.Service

@Service
class GetMyProfileUseCase(
    private val userRepository: UserRepository,
) {
    fun execute(userId: Long): MyProfileResult {
        val user = userRepository.findById(userId) ?: throw UserNotFoundException(userId)
        return MyProfileResult(
            id = requireNotNull(user.id),
            email = user.email,
            nickname = user.nickname,
            acceptsDirectAsk = user.acceptsDirectAsk,
            createdAt = user.createdAt,
        )
    }
}
