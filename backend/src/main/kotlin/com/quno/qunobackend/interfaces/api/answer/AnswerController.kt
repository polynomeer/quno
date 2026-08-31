package com.quno.qunobackend.interfaces.api.answer

import com.quno.qunobackend.application.answer.dto.AcceptAnswerCommand
import com.quno.qunobackend.application.answer.dto.ReviseAnswerCommand
import com.quno.qunobackend.application.answer.dto.WriteAnswerCommand
import com.quno.qunobackend.application.answer.usecase.AcceptAnswerUseCase
import com.quno.qunobackend.application.answer.usecase.GetAnswerVersionDiffUseCase
import com.quno.qunobackend.application.answer.usecase.GetAnswerVersionUseCase
import com.quno.qunobackend.application.answer.usecase.ListAnswerVersionsUseCase
import com.quno.qunobackend.application.answer.usecase.ListAnswersUseCase
import com.quno.qunobackend.application.answer.usecase.ReviseAnswerUseCase
import com.quno.qunobackend.application.answer.usecase.WriteAnswerUseCase
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
@RequestMapping("/api/v1")
class AnswerController(
    private val writeAnswerUseCase: WriteAnswerUseCase,
    private val listAnswersUseCase: ListAnswersUseCase,
    private val acceptAnswerUseCase: AcceptAnswerUseCase,
    private val reviseAnswerUseCase: ReviseAnswerUseCase,
    private val listAnswerVersionsUseCase: ListAnswerVersionsUseCase,
    private val getAnswerVersionUseCase: GetAnswerVersionUseCase,
    private val getAnswerVersionDiffUseCase: GetAnswerVersionDiffUseCase,
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

    @PostMapping("/answers/{answerId}/versions")
    fun revise(
        @AuthenticationPrincipal actorId: Long,
        @PathVariable answerId: Long,
        @Valid @RequestBody request: AnswerContentRequest,
    ): AnswerMutationResponse = reviseAnswerUseCase.execute(
        ReviseAnswerCommand(answerId = answerId, actorId = actorId, body = request.body),
    ).toResponse()

    @GetMapping("/answers/{answerId}/versions")
    fun listVersions(@PathVariable answerId: Long): List<AnswerVersionSummaryResponse> =
        listAnswerVersionsUseCase.execute(answerId).map {
            AnswerVersionSummaryResponse(versionNumber = it.versionNumber, createdBy = it.createdBy, createdAt = it.createdAt)
        }

    @GetMapping("/answers/{answerId}/versions/{version}")
    fun getVersion(@PathVariable answerId: Long, @PathVariable version: Int): AnswerVersionResponse {
        val result = getAnswerVersionUseCase.execute(answerId, version)
        return AnswerVersionResponse(
            answerId = result.answerId,
            versionNumber = result.versionNumber,
            body = result.body,
            createdBy = result.createdBy,
            createdAt = result.createdAt,
        )
    }

    /** Diffs [version] against the version right before it, unless [from] names an earlier one explicitly. */
    @GetMapping("/answers/{answerId}/versions/{version}/diff")
    fun getDiff(
        @PathVariable answerId: Long,
        @PathVariable version: Int,
        @RequestParam(required = false) from: Int?,
    ): AnswerVersionDiffResponse {
        val result = getAnswerVersionDiffUseCase.execute(answerId, fromVersion = from ?: (version - 1), toVersion = version)
        return AnswerVersionDiffResponse(
            fromVersion = result.fromVersion,
            toVersion = result.toVersion,
            lines = result.lines.map { AnswerDiffLineResponse(type = it.type, text = it.text) },
        )
    }
}
