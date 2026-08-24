package com.quno.qunobackend.interfaces.api.question

import com.quno.qunobackend.application.question.dto.CreateQuestionCommand
import com.quno.qunobackend.application.question.dto.QuestionMutationResult
import com.quno.qunobackend.application.question.dto.ReviseQuestionCommand
import com.quno.qunobackend.application.question.usecase.CreateQuestionUseCase
import com.quno.qunobackend.application.question.usecase.GetQuestionUseCase
import com.quno.qunobackend.application.question.usecase.GetQuestionVersionDiffUseCase
import com.quno.qunobackend.application.question.usecase.GetQuestionVersionUseCase
import com.quno.qunobackend.application.question.usecase.ListQuestionVersionsUseCase
import com.quno.qunobackend.application.question.usecase.ReviseQuestionUseCase
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/questions")
class QuestionController(
    private val createQuestionUseCase: CreateQuestionUseCase,
    private val getQuestionUseCase: GetQuestionUseCase,
    private val reviseQuestionUseCase: ReviseQuestionUseCase,
    private val listQuestionVersionsUseCase: ListQuestionVersionsUseCase,
    private val getQuestionVersionUseCase: GetQuestionVersionUseCase,
    private val getQuestionVersionDiffUseCase: GetQuestionVersionDiffUseCase,
) {

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun create(
        @AuthenticationPrincipal authorId: Long,
        @Valid @RequestBody request: QuestionContentRequest,
    ): QuestionMutationResponse {
        val result = createQuestionUseCase.execute(
            CreateQuestionCommand(
                authorId = authorId,
                title = request.title,
                body = request.body,
                environment = request.environment,
                logs = request.logs,
            ),
        )
        return result.toResponse()
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

    @PostMapping("/{id}/versions")
    fun revise(
        @AuthenticationPrincipal actorId: Long,
        @PathVariable id: Long,
        @Valid @RequestBody request: QuestionContentRequest,
    ): QuestionMutationResponse {
        val result = reviseQuestionUseCase.execute(
            ReviseQuestionCommand(
                questionId = id,
                actorId = actorId,
                title = request.title,
                body = request.body,
                environment = request.environment,
                logs = request.logs,
            ),
        )
        return result.toResponse()
    }

    @GetMapping("/{id}/versions")
    fun listVersions(@PathVariable id: Long): List<QuestionVersionSummaryResponse> =
        listQuestionVersionsUseCase.execute(id).map {
            QuestionVersionSummaryResponse(
                versionNumber = it.versionNumber,
                title = it.title,
                createdBy = it.createdBy,
                createdAt = it.createdAt,
            )
        }

    @GetMapping("/{id}/versions/{version}")
    fun getVersion(@PathVariable id: Long, @PathVariable version: Int): QuestionVersionResponse {
        val result = getQuestionVersionUseCase.execute(id, version)
        return QuestionVersionResponse(
            questionId = result.questionId,
            versionNumber = result.versionNumber,
            title = result.title,
            body = result.body,
            environment = result.environment,
            logs = result.logs,
            createdBy = result.createdBy,
            createdAt = result.createdAt,
        )
    }

    /** Diffs [version] against the version right before it, unless [from] names an earlier one explicitly. */
    @GetMapping("/{id}/versions/{version}/diff")
    fun getDiff(
        @PathVariable id: Long,
        @PathVariable version: Int,
        @RequestParam(required = false) from: Int?,
    ): QuestionVersionDiffResponse {
        val result = getQuestionVersionDiffUseCase.execute(id, fromVersion = from ?: (version - 1), toVersion = version)
        return QuestionVersionDiffResponse(
            fromVersion = result.fromVersion,
            toVersion = result.toVersion,
            lines = result.lines.map { DiffLineResponse(type = it.type, text = it.text) },
        )
    }

    private fun QuestionMutationResult.toResponse() =
        QuestionMutationResponse(id = id, title = title, status = status, versionNumber = versionNumber)
}
