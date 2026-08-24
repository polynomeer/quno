package com.quno.qunobackend.interfaces.api.user

import com.quno.qunobackend.application.user.usecase.GetMyProfileUseCase
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/me")
class UserController(
    private val getMyProfileUseCase: GetMyProfileUseCase,
) {

    @GetMapping
    fun getMyProfile(@AuthenticationPrincipal userId: Long): MyProfileResponse {
        val result = getMyProfileUseCase.execute(userId)
        return MyProfileResponse(
            id = result.id,
            email = result.email,
            nickname = result.nickname,
            createdAt = result.createdAt,
        )
    }
}
