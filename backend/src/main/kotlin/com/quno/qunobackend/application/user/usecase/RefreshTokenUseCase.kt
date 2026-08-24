package com.quno.qunobackend.application.user.usecase

import com.quno.qunobackend.application.user.TokenProvider
import com.quno.qunobackend.application.user.dto.RefreshTokenCommand
import com.quno.qunobackend.application.user.dto.TokenResult
import com.quno.qunobackend.domain.user.InvalidCredentialsException
import com.quno.qunobackend.domain.user.UserNotFoundException
import com.quno.qunobackend.domain.user.UserRepository
import org.springframework.stereotype.Service

@Service
class RefreshTokenUseCase(
    private val userRepository: UserRepository,
    private val tokenProvider: TokenProvider,
) {
    fun execute(command: RefreshTokenCommand): TokenResult {
        val userId = tokenProvider.validateRefreshToken(command.refreshToken)
        val user = userRepository.findById(userId) ?: throw UserNotFoundException(userId)
        if (!user.isActive) throw InvalidCredentialsException()

        return TokenResult(
            accessToken = tokenProvider.generateAccessToken(userId),
            refreshToken = tokenProvider.generateRefreshToken(userId),
        )
    }
}
