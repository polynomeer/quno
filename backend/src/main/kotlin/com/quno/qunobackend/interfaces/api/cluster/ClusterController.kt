package com.quno.qunobackend.interfaces.api.cluster

import com.quno.qunobackend.application.cluster.usecase.GetClusterUseCase
import com.quno.qunobackend.application.cluster.usecase.MarkQuestionsAsSameProblemUseCase
import jakarta.validation.Valid
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
) {

    @PostMapping
    fun markAsSameProblem(
        @PathVariable questionId: Long,
        @Valid @RequestBody request: MarkAsSameProblemRequest,
    ): ClusterResponse =
        markQuestionsAsSameProblemUseCase.execute(questionId, request.relatedQuestionId).toResponse()

    @GetMapping
    fun getForQuestion(@PathVariable questionId: Long): ClusterDetailResponse =
        getClusterUseCase.executeForQuestion(questionId).toResponse()
}
