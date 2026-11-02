package com.quno.qunobackend.interfaces.api.cluster

import com.quno.qunobackend.application.cluster.usecase.DesignateSuperAnswerUseCase
import com.quno.qunobackend.application.cluster.usecase.GetClusterUseCase
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
@RequestMapping("/api/v1/clusters/{clusterId}")
class ClusterDetailController(
    private val getClusterUseCase: GetClusterUseCase,
    private val designateSuperAnswerUseCase: DesignateSuperAnswerUseCase,
    private val access: QuestionAccess,
) {

    @GetMapping
    fun get(@AuthenticationPrincipal viewerId: Long?, @PathVariable clusterId: Long): ClusterDetailResponse = getClusterUseCase.execute(clusterId, access.viewer(viewerId)).toResponse()

    @PostMapping("/super-answer")
    fun designateSuperAnswer(
        @AuthenticationPrincipal viewerId: Long,
        @PathVariable clusterId: Long,
        @Valid @RequestBody request: DesignateSuperAnswerRequest,
    ): ClusterDetailResponse = designateSuperAnswerUseCase.execute(clusterId, request.answerId, access.viewer(viewerId)).toResponse()
}
