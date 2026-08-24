package com.quno.qunobackend.interfaces.api.question

import com.quno.qunobackend.application.question.dto.CreateQuestionCommand
import com.quno.qunobackend.application.question.usecase.CreateQuestionUseCase
import com.quno.qunobackend.application.question.usecase.GetQuestionUseCase
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
@RequestMapping("/api/v1/questions")
class QuestionController(
    private val createQuestionUseCase: CreateQuestionUseCase,
    private val getQuestionUseCase: GetQuestionUseCase,
) {

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun create(
        @AuthenticationPrincipal authorId: Long,
        @Valid @RequestBody request: CreateQuestionRequest,
    ): CreateQuestionResponse {
        val result = createQuestionUseCase.execute(
            CreateQuestionCommand(
                authorId = authorId,
                title = request.title,
                body = request.body,
                environment = request.environment,
                logs = request.logs,
            ),
        )
        return CreateQuestionResponse(
            id = result.id,
            title = result.title,
            status = result.status,
            versionNumber = result.versionNumber,
        )
    }

    @GetMapping("/{id}")
    fun get(@PathVariable id: Long): QuestionResponse {
        val result = getQuestionUseCase.execute(id)
        return QuestionResponse(
            id = result.id,
            authorId = result.authorId,
            title = result.title,
            status = result.status,
            versionNumber = result.versionNumber,
            body = result.body,
            environment = result.environment,
            logs = result.logs,
            createdAt = result.createdAt,
            updatedAt = result.updatedAt,
        )
    }
}
