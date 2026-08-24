package com.quno.qunobackend.application.user.usecase

import com.quno.qunobackend.application.user.TokenProvider
import com.quno.qunobackend.application.user.dto.LoginCommand
import com.quno.qunobackend.application.user.dto.TokenResult
import com.quno.qunobackend.domain.user.InvalidCredentialsException
import com.quno.qunobackend.domain.user.UserRepository
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service

@Service
class LoginUseCase(
    private val userRepository: UserRepository,
    private val passwordEncoder: PasswordEncoder,
    private val tokenProvider: TokenProvider,
) {
    fun execute(command: LoginCommand): TokenResult {
        val user = userRepository.findByEmail(command.email) ?: throw InvalidCredentialsException()
        if (!user.isActive || !passwordEncoder.matches(command.rawPassword, user.passwordHash)) {
            throw InvalidCredentialsException()
        }
        val userId = requireNotNull(user.id)
        return TokenResult(
            accessToken = tokenProvider.generateAccessToken(userId),
            refreshToken = tokenProvider.generateRefreshToken(userId),
        )
    }
}
