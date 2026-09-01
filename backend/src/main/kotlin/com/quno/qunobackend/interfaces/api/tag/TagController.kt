package com.quno.qunobackend.interfaces.api.tag

import com.quno.qunobackend.application.tag.usecase.FollowTagUseCase
import com.quno.qunobackend.application.tag.usecase.GetTagUseCase
import com.quno.qunobackend.application.tag.usecase.ListRelatedTagsUseCase
import com.quno.qunobackend.application.tag.usecase.ListTagContributorsUseCase
import com.quno.qunobackend.application.tag.usecase.ListTagQuestionsUseCase
import com.quno.qunobackend.application.tag.usecase.SearchTagsUseCase
import com.quno.qunobackend.application.tag.usecase.UnfollowTagUseCase
import com.quno.qunobackend.application.tag.usecase.UpdateTagDetailsUseCase
import com.quno.qunobackend.domain.tag.TagQuestionSort
import com.quno.qunobackend.interfaces.api.search.QuestionSearchResultResponse
import com.quno.qunobackend.interfaces.api.search.toResponse
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/tags")
class TagController(
    private val searchTagsUseCase: SearchTagsUseCase,
    private val getTagUseCase: GetTagUseCase,
    private val updateTagDetailsUseCase: UpdateTagDetailsUseCase,
    private val listTagQuestionsUseCase: ListTagQuestionsUseCase,
    private val listTagContributorsUseCase: ListTagContributorsUseCase,
    private val listRelatedTagsUseCase: ListRelatedTagsUseCase,
    private val followTagUseCase: FollowTagUseCase,
    private val unfollowTagUseCase: UnfollowTagUseCase,
) {

    @GetMapping
    fun search(@RequestParam(required = false) q: String?): List<TagResponse> =
        searchTagsUseCase.execute(q).map { it.toResponse() }

    @GetMapping("/{id}")
    fun get(@PathVariable id: Long): TagResponse = getTagUseCase.execute(id).toResponse()

    /** Wiki-style — no ownership check, any authenticated user can edit (Phase 28, ADR-0040). */
    @PutMapping("/{id}")
    fun updateDetails(@PathVariable id: Long, @Valid @RequestBody request: UpdateTagDetailsRequest): TagResponse =
        updateTagDetailsUseCase.execute(id, request.description, request.docsUrl).toResponse()

    @GetMapping("/{id}/questions")
    fun questions(
        @PathVariable id: Long,
        @RequestParam(required = false) sort: String?,
        @RequestParam(required = false) limit: Int?,
    ): List<QuestionSearchResultResponse> {
        val resolvedSort = TagQuestionSort.entries.find { it.name.equals(sort, ignoreCase = true) } ?: TagQuestionSort.LATEST
        return listTagQuestionsUseCase.execute(id, resolvedSort, limit ?: 20).map { it.toResponse() }
    }

    @GetMapping("/{id}/contributors")
    fun contributors(@PathVariable id: Long, @RequestParam(required = false) limit: Int?): List<TagContributorResponse> =
        listTagContributorsUseCase.execute(id, limit ?: 10).map { it.toResponse() }

    @GetMapping("/{id}/related")
    fun related(@PathVariable id: Long, @RequestParam(required = false) limit: Int?): List<TagResponse> =
        listRelatedTagsUseCase.execute(id, limit ?: 10).map { it.toResponse() }

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
