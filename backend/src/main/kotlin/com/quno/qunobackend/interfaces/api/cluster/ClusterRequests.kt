package com.quno.qunobackend.interfaces.api.cluster

data class MarkAsSameProblemRequest(
    val relatedQuestionId: Long,
)

data class DesignateSuperAnswerRequest(
    val answerId: Long,
)
