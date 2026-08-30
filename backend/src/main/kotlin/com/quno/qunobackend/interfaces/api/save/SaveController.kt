package com.quno.qunobackend.interfaces.api.save

import com.quno.qunobackend.application.save.usecase.ListMySavesUseCase
import com.quno.qunobackend.application.save.usecase.SaveQuestionUseCase
import com.quno.qunobackend.application.save.usecase.UnsaveQuestionUseCase
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
class SaveController(
    private val saveQuestionUseCase: SaveQuestionUseCase,
    private val unsaveQuestionUseCase: UnsaveQuestionUseCase,
    private val listMySavesUseCase: ListMySavesUseCase,
) {

    @PostMapping("/questions/{questionId}/save")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun save(@AuthenticationPrincipal userId: Long, @PathVariable questionId: Long) {
        saveQuestionUseCase.execute(userId, questionId)
    }

    @DeleteMapping("/questions/{questionId}/save")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun unsave(@AuthenticationPrincipal userId: Long, @PathVariable questionId: Long) {
        unsaveQuestionUseCase.execute(userId, questionId)
    }

    @GetMapping("/me/saves")
    fun mySaves(@AuthenticationPrincipal userId: Long): List<SavedQuestionResponse> =
        listMySavesUseCase.execute(userId).map {
            SavedQuestionResponse(questionId = it.questionId, title = it.title, status = it.status)
        }
}
