package com.quno.qunobackend.interfaces.api.answer

import com.quno.qunobackend.application.answer.dto.AcceptAnswerCommand
import com.quno.qunobackend.application.answer.dto.WriteAnswerCommand
import com.quno.qunobackend.application.answer.usecase.AcceptAnswerUseCase
import com.quno.qunobackend.application.answer.usecase.ListAnswersUseCase
import com.quno.qunobackend.application.answer.usecase.WriteAnswerUseCase
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1")
class AnswerController(
    private val writeAnswerUseCase: WriteAnswerUseCase,
    private val listAnswersUseCase: ListAnswersUseCase,
    private val acceptAnswerUseCase: AcceptAnswerUseCase,
) {

    @PostMapping("/questions/{questionId}/answers")
    @ResponseStatus(HttpStatus.CREATED)
    fun write(
        @AuthenticationPrincipal authorId: Long,
        @PathVariable questionId: Long,
        @Valid @RequestBody request: WriteAnswerRequest,
    ): AnswerResponse {
        val result = writeAnswerUseCase.execute(
            WriteAnswerCommand(questionId = questionId, authorId = authorId, body = request.body),
        )
        return result.toResponse()
    }

    @GetMapping("/questions/{questionId}/answers")
    fun list(@PathVariable questionId: Long): List<AnswerResponse> =
        listAnswersUseCase.execute(questionId).map { it.toResponse() }

    @PostMapping("/answers/{answerId}/accept")
    fun accept(@AuthenticationPrincipal actorId: Long, @PathVariable answerId: Long): AcceptAnswerResponse {
        val result = acceptAnswerUseCase.execute(AcceptAnswerCommand(answerId = answerId, actorId = actorId))
        return AcceptAnswerResponse(
            questionId = result.questionId,
            answerId = result.answerId,
            questionStatus = result.questionStatus,
        )
    }
}
