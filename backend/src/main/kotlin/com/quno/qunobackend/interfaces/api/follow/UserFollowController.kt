package com.quno.qunobackend.interfaces.api.follow

import com.quno.qunobackend.application.follow.usecase.FollowUserUseCase
import com.quno.qunobackend.application.follow.usecase.ListMyFollowingUseCase
import com.quno.qunobackend.application.follow.usecase.UnfollowUserUseCase
import org.springframework.http.HttpStatus
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1")
class UserFollowController(
    private val followUserUseCase: FollowUserUseCase,
    private val unfollowUserUseCase: UnfollowUserUseCase,
    private val listMyFollowingUseCase: ListMyFollowingUseCase,
) {

    @PostMapping("/users/{userId}/follow")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun follow(@AuthenticationPrincipal followerId: Long, @PathVariable userId: Long) {
        followUserUseCase.execute(followerId, userId)
    }

    @DeleteMapping("/users/{userId}/follow")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun unfollow(@AuthenticationPrincipal followerId: Long, @PathVariable userId: Long) {
        unfollowUserUseCase.execute(followerId, userId)
    }

    @GetMapping("/me/following")
    fun myFollowing(@AuthenticationPrincipal followerId: Long): List<FolloweeResponse> =
        listMyFollowingUseCase.execute(followerId).map {
            FolloweeResponse(userId = it.userId, nickname = it.nickname)
        }
}
