package com.quno.qunobackend.interfaces.api.vote

import com.quno.qunobackend.application.vote.dto.VoteResult
import com.quno.qunobackend.domain.vote.VoteTargetType

data class VoteResponse(
    val targetType: VoteTargetType,
    val targetId: Long,
    val value: Int,
)

fun VoteResult.toResponse() = VoteResponse(targetType = targetType, targetId = targetId, value = value)
