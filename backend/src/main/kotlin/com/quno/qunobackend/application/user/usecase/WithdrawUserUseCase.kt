package com.quno.qunobackend.application.user.usecase

import com.quno.qunobackend.application.user.dto.WithdrawUserCommand
import com.quno.qunobackend.domain.user.InvalidCredentialsException
import com.quno.qunobackend.domain.user.UserNotFoundException
import com.quno.qunobackend.domain.user.UserRepository
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import java.util.UUID

/** 회원 탈퇴(ADR-0046) — 계정 자체를 지우지 않고 PII만 익명화한다. 도용된 access token만으로
 * 탈퇴시키는 것을 막기 위해 현재 비밀번호 재확인을 요구한다(로그인과 같은 검증). */
@Service
class WithdrawUserUseCase(
    private val userRepository: UserRepository,
    private val passwordEncoder: PasswordEncoder,
) {
    fun execute(command: WithdrawUserCommand) {
        val user = userRepository.findById(command.userId) ?: throw UserNotFoundException(command.userId)
        if (!passwordEncoder.matches(command.rawPassword, user.passwordHash)) {
            throw InvalidCredentialsException()
        }
        // 로그인 불가능한 무작위 해시로 교체한다 — 빈 문자열 등 형식이 이상한 해시를 넣으면
        // BCryptPasswordEncoder.matches()가 예외를 던질 수 있어(isActive 체크가 항상 먼저
        // 평가되긴 하지만) 항상 유효한 해시 포맷을 유지하는 편이 더 안전하다.
        val randomPasswordHash = requireNotNull(passwordEncoder.encode(UUID.randomUUID().toString()))
        userRepository.save(user.withdraw(randomPasswordHash))
    }
}
