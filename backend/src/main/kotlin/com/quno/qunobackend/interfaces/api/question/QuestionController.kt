package com.quno.qunobackend.interfaces.api.question

import com.quno.qunobackend.application.cluster.usecase.GetQuestionGraphUseCase
import com.quno.qunobackend.application.question.dto.CreateQuestionCommand
import com.quno.qunobackend.application.question.dto.ForkQuestionCommand
import com.quno.qunobackend.application.question.dto.MarkQuestionOutdatedCommand
import com.quno.qunobackend.application.question.dto.QuestionMutationResult
import com.quno.qunobackend.application.question.dto.ReviseQuestionCommand
import com.quno.qunobackend.application.question.usecase.CreateQuestionUseCase
import com.quno.qunobackend.application.question.usecase.ForkQuestionUseCase
import com.quno.qunobackend.application.question.usecase.GetQuestionTimelineUseCase
import com.quno.qunobackend.application.question.usecase.GetQuestionUseCase
import com.quno.qunobackend.application.question.usecase.GetQuestionVersionDiffUseCase
import com.quno.qunobackend.application.question.usecase.GetQuestionVersionUseCase
import com.quno.qunobackend.application.question.usecase.ListQuestionForksUseCase
import com.quno.qunobackend.application.question.usecase.ListQuestionVersionsUseCase
import com.quno.qunobackend.application.question.usecase.MarkQuestionOutdatedUseCase
import com.quno.qunobackend.application.question.usecase.RecordQuestionViewUseCase
import com.quno.qunobackend.application.question.usecase.ReviseQuestionUseCase
import com.quno.qunobackend.application.search.usecase.QuestionSearchUseCase
import com.quno.qunobackend.interfaces.api.search.QuestionSearchResultResponse
import com.quno.qunobackend.interfaces.api.search.toResponse
import jakarta.servlet.http.HttpServletRequest
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
import java.security.MessageDigest

@RestController
@RequestMapping("/api/v1/questions")
class QuestionController(
    private val createQuestionUseCase: CreateQuestionUseCase,
    private val getQuestionUseCase: GetQuestionUseCase,
    private val reviseQuestionUseCase: ReviseQuestionUseCase,
    private val listQuestionVersionsUseCase: ListQuestionVersionsUseCase,
    private val getQuestionVersionUseCase: GetQuestionVersionUseCase,
    private val getQuestionVersionDiffUseCase: GetQuestionVersionDiffUseCase,
    private val questionSearchUseCase: QuestionSearchUseCase,
    private val markQuestionOutdatedUseCase: MarkQuestionOutdatedUseCase,
    private val forkQuestionUseCase: ForkQuestionUseCase,
    private val listQuestionForksUseCase: ListQuestionForksUseCase,
    private val getQuestionGraphUseCase: GetQuestionGraphUseCase,
    private val getQuestionTimelineUseCase: GetQuestionTimelineUseCase,
    private val recordQuestionViewUseCase: RecordQuestionViewUseCase,
) {

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun create(
        @AuthenticationPrincipal authorId: Long,
        @Valid @RequestBody request: CreateQuestionRequest,
    ): QuestionMutationResponse {
        val result = createQuestionUseCase.execute(
            CreateQuestionCommand(
                authorId = authorId,
                title = request.title,
                body = request.body,
                environment = request.environment,
                logs = request.logs,
                tagNames = request.tags,
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
            tags = result.tags,
            score = result.score,
            viewCount = result.viewCount,
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
    fun listVersions(@PathVariable id: Long): List<QuestionVersionSummaryResponse> = listQuestionVersionsUseCase.execute(id).map {
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

    /**
     * Counts one view (ADR-0063). Public like the detail page itself; a signed-in viewer is keyed
     * by user id, an anonymous one by a hash of IP + User-Agent so the raw address isn't stored.
     */
    @PostMapping("/{id}/views")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun recordView(
        @AuthenticationPrincipal viewerId: Long?,
        @PathVariable id: Long,
        request: HttpServletRequest,
    ) {
        val viewerKey = viewerId?.let { "u:$it" } ?: anonymousViewerKey(request)
        recordQuestionViewUseCase.execute(id, viewerKey)
    }

    /** "질문의 생애" panel (ADR-0062) — newest first. */
    @GetMapping("/{id}/timeline")
    fun timeline(@PathVariable id: Long): List<QuestionTimelineEventResponse> = getQuestionTimelineUseCase.execute(id).map {
        QuestionTimelineEventResponse(
            type = it.type,
            occurredAt = it.occurredAt,
            actorId = it.actorId,
            versionNumber = it.versionNumber,
            answerId = it.answerId,
            accepted = it.accepted,
        )
    }

    @GetMapping("/{id}/related")
    fun related(
        @PathVariable id: Long,
        @RequestParam(required = false) limit: Int?,
    ): List<QuestionSearchResultResponse> = questionSearchUseCase.related(id, limit ?: 5).map { it.toResponse() }

    @PostMapping("/{id}/outdated")
    fun markOutdated(
        @AuthenticationPrincipal actorId: Long,
        @PathVariable id: Long,
        @Valid @RequestBody request: MarkQuestionOutdatedRequest,
    ): QuestionMutationResponse {
        val result = markQuestionOutdatedUseCase.execute(
            MarkQuestionOutdatedCommand(questionId = id, actorId = actorId, reason = request.reason),
        )
        return result.toResponse()
    }

    @PostMapping("/{id}/fork")
    @ResponseStatus(HttpStatus.CREATED)
    fun fork(@AuthenticationPrincipal actorId: Long, @PathVariable id: Long): QuestionMutationResponse = forkQuestionUseCase.execute(ForkQuestionCommand(originQuestionId = id, actorId = actorId)).toResponse()

    @GetMapping("/{id}/forks")
    fun forks(@PathVariable id: Long): List<QuestionSearchResultResponse> = listQuestionForksUseCase.execute(id).map { it.toResponse() }

    @GetMapping("/{id}/graph")
    fun graph(@PathVariable id: Long): QuestionGraphResponse = getQuestionGraphUseCase.execute(id).toResponse()

    private fun anonymousViewerKey(request: HttpServletRequest): String {
        val raw = "${request.remoteAddr}|${request.getHeader("User-Agent").orEmpty()}"
        val digest = MessageDigest.getInstance("SHA-256").digest(raw.toByteArray())
        return "a:" + digest.take(16).joinToString("") { "%02x".format(it) }
    }

    private fun QuestionMutationResult.toResponse() = QuestionMutationResponse(id = id, title = title, status = status, versionNumber = versionNumber)
}
