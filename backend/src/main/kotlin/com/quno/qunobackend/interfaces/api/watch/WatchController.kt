package com.quno.qunobackend.interfaces.api.watch

import com.quno.qunobackend.application.watch.usecase.ListMyWatchesUseCase
import com.quno.qunobackend.application.watch.usecase.UnwatchQuestionUseCase
import com.quno.qunobackend.application.watch.usecase.WatchQuestionUseCase
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
class WatchController(
    private val watchQuestionUseCase: WatchQuestionUseCase,
    private val unwatchQuestionUseCase: UnwatchQuestionUseCase,
    private val listMyWatchesUseCase: ListMyWatchesUseCase,
) {

    @PostMapping("/questions/{questionId}/watch")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun watch(@AuthenticationPrincipal userId: Long, @PathVariable questionId: Long) {
        watchQuestionUseCase.execute(userId, questionId)
    }

    @DeleteMapping("/questions/{questionId}/watch")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun unwatch(@AuthenticationPrincipal userId: Long, @PathVariable questionId: Long) {
        unwatchQuestionUseCase.execute(userId, questionId)
    }

    @GetMapping("/me/watches")
    fun myWatches(@AuthenticationPrincipal userId: Long): List<WatchedQuestionResponse> =
        listMyWatchesUseCase.execute(userId).map {
            WatchedQuestionResponse(questionId = it.questionId, title = it.title, status = it.status)
        }
}
