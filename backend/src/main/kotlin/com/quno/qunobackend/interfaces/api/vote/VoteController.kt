package com.quno.qunobackend.interfaces.api.vote

import com.quno.qunobackend.application.vote.dto.CastVoteCommand
import com.quno.qunobackend.application.vote.usecase.CastVoteUseCase
import com.quno.qunobackend.application.vote.usecase.ListMyVotesUseCase
import com.quno.qunobackend.application.vote.usecase.RetractVoteUseCase
import com.quno.qunobackend.domain.vote.VoteTargetType
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

/** Cast/retract return 204, same as WatchController — score is read back via the question/answer
 * response itself (Phase 11.3) or `GET /me/votes` for "did I vote on this", not from this call. */
@RestController
@RequestMapping("/api/v1")
class VoteController(
    private val castVoteUseCase: CastVoteUseCase,
    private val retractVoteUseCase: RetractVoteUseCase,
    private val listMyVotesUseCase: ListMyVotesUseCase,
) {

    @PostMapping("/questions/{questionId}/vote")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun voteOnQuestion(
        @AuthenticationPrincipal voterId: Long,
        @PathVariable questionId: Long,
        @Valid @RequestBody request: CastVoteRequest,
    ) {
        castVoteUseCase.execute(CastVoteCommand(voterId, VoteTargetType.QUESTION, questionId, request.value))
    }

    @DeleteMapping("/questions/{questionId}/vote")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun retractQuestionVote(@AuthenticationPrincipal voterId: Long, @PathVariable questionId: Long) {
        retractVoteUseCase.execute(voterId, VoteTargetType.QUESTION, questionId)
    }

    @PostMapping("/answers/{answerId}/vote")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun voteOnAnswer(
        @AuthenticationPrincipal voterId: Long,
        @PathVariable answerId: Long,
        @Valid @RequestBody request: CastVoteRequest,
    ) {
        castVoteUseCase.execute(CastVoteCommand(voterId, VoteTargetType.ANSWER, answerId, request.value))
    }

    @DeleteMapping("/answers/{answerId}/vote")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun retractAnswerVote(@AuthenticationPrincipal voterId: Long, @PathVariable answerId: Long) {
        retractVoteUseCase.execute(voterId, VoteTargetType.ANSWER, answerId)
    }

    @GetMapping("/me/votes")
    fun myVotes(@AuthenticationPrincipal voterId: Long): List<VoteResponse> =
        listMyVotesUseCase.execute(voterId).map { it.toResponse() }
}
