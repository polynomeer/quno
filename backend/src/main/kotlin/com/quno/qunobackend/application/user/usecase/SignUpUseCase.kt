package com.quno.qunobackend.application.user.usecase

import com.quno.qunobackend.application.user.dto.SignUpCommand
import com.quno.qunobackend.application.user.dto.SignUpResult
import com.quno.qunobackend.domain.user.DuplicateEmailException
import com.quno.qunobackend.domain.user.DuplicateNicknameException
import com.quno.qunobackend.domain.user.User
import com.quno.qunobackend.domain.user.UserRepository
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class SignUpUseCase(
    private val userRepository: UserRepository,
    private val passwordEncoder: PasswordEncoder,
) {
    @Transactional
    fun execute(command: SignUpCommand): SignUpResult {
        if (userRepository.existsByEmail(command.email)) throw DuplicateEmailException(command.email)
        if (userRepository.existsByNickname(command.nickname)) throw DuplicateNicknameException(command.nickname)

        val user = User.register(
            email = command.email,
            nickname = command.nickname,
            passwordHash = requireNotNull(passwordEncoder.encode(command.rawPassword)),
        )
        val saved = userRepository.save(user)
        return SignUpResult(userId = requireNotNull(saved.id), email = saved.email, nickname = saved.nickname)
    }
}
