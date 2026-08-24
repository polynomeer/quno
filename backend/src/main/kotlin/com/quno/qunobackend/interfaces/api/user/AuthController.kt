package com.quno.qunobackend.interfaces.api.user

import com.quno.qunobackend.application.user.dto.LoginCommand
import com.quno.qunobackend.application.user.dto.RefreshTokenCommand
import com.quno.qunobackend.application.user.dto.SignUpCommand
import com.quno.qunobackend.application.user.usecase.LoginUseCase
import com.quno.qunobackend.application.user.usecase.RefreshTokenUseCase
import com.quno.qunobackend.application.user.usecase.SignUpUseCase
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/auth")
class AuthController(
    private val signUpUseCase: SignUpUseCase,
    private val loginUseCase: LoginUseCase,
    private val refreshTokenUseCase: RefreshTokenUseCase,
) {

    @PostMapping("/signup")
    @ResponseStatus(HttpStatus.CREATED)
    fun signUp(@Valid @RequestBody request: SignUpRequest): SignUpResponse {
        val result = signUpUseCase.execute(
            SignUpCommand(email = request.email, nickname = request.nickname, rawPassword = request.password),
        )
        return SignUpResponse(id = result.userId, email = result.email, nickname = result.nickname)
    }

    @PostMapping("/login")
    fun login(@Valid @RequestBody request: LoginRequest): TokenResponse {
        val result = loginUseCase.execute(LoginCommand(email = request.email, rawPassword = request.password))
        return TokenResponse(accessToken = result.accessToken, refreshToken = result.refreshToken)
    }

    @PostMapping("/refresh")
    fun refresh(@Valid @RequestBody request: RefreshRequest): TokenResponse {
        val result = refreshTokenUseCase.execute(RefreshTokenCommand(refreshToken = request.refreshToken))
        return TokenResponse(accessToken = result.accessToken, refreshToken = result.refreshToken)
    }
}
