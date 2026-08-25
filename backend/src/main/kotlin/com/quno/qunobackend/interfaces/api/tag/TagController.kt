package com.quno.qunobackend.interfaces.api.tag

import com.quno.qunobackend.application.tag.usecase.FollowTagUseCase
import com.quno.qunobackend.application.tag.usecase.SearchTagsUseCase
import com.quno.qunobackend.application.tag.usecase.UnfollowTagUseCase
import org.springframework.http.HttpStatus
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/tags")
class TagController(
    private val searchTagsUseCase: SearchTagsUseCase,
    private val followTagUseCase: FollowTagUseCase,
    private val unfollowTagUseCase: UnfollowTagUseCase,
) {

    @GetMapping
    fun search(@RequestParam(required = false) q: String?): List<TagResponse> =
        searchTagsUseCase.execute(q).map { it.toResponse() }

    @PostMapping("/{id}/follow")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun follow(@AuthenticationPrincipal userId: Long, @PathVariable id: Long) {
        followTagUseCase.execute(userId, id)
    }

    @DeleteMapping("/{id}/follow")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun unfollow(@AuthenticationPrincipal userId: Long, @PathVariable id: Long) {
        unfollowTagUseCase.execute(userId, id)
    }
}
