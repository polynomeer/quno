package com.quno.qunobackend.interfaces.api.user

import com.quno.qunobackend.application.user.usecase.GetUserProfileUseCase
import com.quno.qunobackend.interfaces.api.answer.toResponse
import com.quno.qunobackend.interfaces.api.search.toResponse
import com.quno.qunobackend.interfaces.api.tag.toResponse
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

/** Public profile — separate from [UserController]'s private `/me` (which includes email). */
@RestController
@RequestMapping("/api/v1/users")
class UserProfileController(
    private val getUserProfileUseCase: GetUserProfileUseCase,
) {

    @GetMapping("/{id}/profile")
    fun getProfile(@PathVariable id: Long): UserProfileResponse {
        val result = getUserProfileUseCase.execute(id)
        return UserProfileResponse(
            userId = result.userId,
            nickname = result.nickname,
            questions = result.questions.map { it.toResponse() },
            answers = result.answers.map { it.toResponse() },
            followedTags = result.followedTags.map { it.toResponse() },
        )
    }
}
