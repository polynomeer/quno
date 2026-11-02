package com.quno.qunobackend.interfaces.api.cluster

import com.quno.qunobackend.application.cluster.usecase.GetClusterUseCase
import com.quno.qunobackend.application.cluster.usecase.MarkQuestionsAsSameProblemUseCase
import com.quno.qunobackend.application.common.QuestionAccess
import jakarta.validation.Valid
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/questions/{questionId}/cluster")
class ClusterController(
    private val markQuestionsAsSameProblemUseCase: MarkQuestionsAsSameProblemUseCase,
    private val getClusterUseCase: GetClusterUseCase,
    private val access: QuestionAccess,
) {

    @PostMapping
    fun markAsSameProblem(
        @AuthenticationPrincipal viewerId: Long,
        @PathVariable questionId: Long,
        @Valid @RequestBody request: MarkAsSameProblemRequest,
    ): ClusterResponse = markQuestionsAsSameProblemUseCase.execute(questionId, request.relatedQuestionId, access.viewer(viewerId)).toResponse()

    @GetMapping
    fun getForQuestion(@AuthenticationPrincipal viewerId: Long?, @PathVariable questionId: Long): ClusterDetailResponse = getClusterUseCase.executeForQuestion(questionId, access.viewer(viewerId)).toResponse()
}
