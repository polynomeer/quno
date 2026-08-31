package com.quno.qunobackend.interfaces.api.user

import com.quno.qunobackend.application.user.usecase.GetMyProfileUseCase
import com.quno.qunobackend.application.user.usecase.UpdateDirectAskSettingsUseCase
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/me")
class UserController(
    private val getMyProfileUseCase: GetMyProfileUseCase,
    private val updateDirectAskSettingsUseCase: UpdateDirectAskSettingsUseCase,
) {

    @GetMapping
    fun getMyProfile(@AuthenticationPrincipal userId: Long): MyProfileResponse {
        val result = getMyProfileUseCase.execute(userId)
        return MyProfileResponse(
            id = result.id,
            email = result.email,
            nickname = result.nickname,
            acceptsDirectAsk = result.acceptsDirectAsk,
            createdAt = result.createdAt,
        )
    }

    @PutMapping("/direct-ask-settings")
    fun updateDirectAskSettings(@AuthenticationPrincipal userId: Long, @RequestBody request: DirectAskSettingsRequest): MyProfileResponse {
        updateDirectAskSettingsUseCase.execute(userId, request.accepts)
        return getMyProfile(userId)
    }
}

data class DirectAskSettingsRequest(val accepts: Boolean)
