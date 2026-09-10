package com.quno.qunobackend.interfaces.api.user

import com.quno.qunobackend.application.user.dto.WithdrawUserCommand
import com.quno.qunobackend.application.user.usecase.ExportMyDataUseCase
import com.quno.qunobackend.application.user.usecase.GetMyProfileUseCase
import com.quno.qunobackend.application.user.usecase.UpdateDirectAskSettingsUseCase
import com.quno.qunobackend.application.user.usecase.WithdrawUserUseCase
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.DeleteMapping
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
    private val withdrawUserUseCase: WithdrawUserUseCase,
    private val exportMyDataUseCase: ExportMyDataUseCase,
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

    /** 회원 탈퇴(ADR-0046). 계정 row는 남고 PII만 익명화된다 — 도용된 access token만으로 탈퇴시키는
     * 것을 막기 위해 현재 비밀번호 재확인을 요구한다. */
    @DeleteMapping
    fun withdraw(@AuthenticationPrincipal userId: Long, @RequestBody request: WithdrawRequest) {
        withdrawUserUseCase.execute(WithdrawUserCommand(userId, request.password))
    }

    /** 개인정보 다운로드 요청(ADR-0046) — 최소 요건: 프로필 + 직접 작성한 질문/답변. */
    @GetMapping("/data-export")
    fun exportMyData(@AuthenticationPrincipal userId: Long): ResponseEntity<Any> {
        val result = exportMyDataUseCase.execute(userId)
        return ResponseEntity.status(HttpStatus.OK)
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"quno-my-data.json\"")
            .body(result)
    }
}

data class DirectAskSettingsRequest(val accepts: Boolean)

data class WithdrawRequest(val password: String)
