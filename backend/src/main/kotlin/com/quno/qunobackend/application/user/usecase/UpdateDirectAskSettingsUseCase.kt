package com.quno.qunobackend.application.user.usecase

import com.quno.qunobackend.domain.user.UserNotFoundException
import com.quno.qunobackend.domain.user.UserRepository
import org.springframework.stereotype.Service

@Service
class UpdateDirectAskSettingsUseCase(
    private val userRepository: UserRepository,
) {
    fun execute(userId: Long, accepts: Boolean) {
        val user = userRepository.findById(userId) ?: throw UserNotFoundException(userId)
        userRepository.save(user.updateDirectAskSettings(accepts))
    }
}
