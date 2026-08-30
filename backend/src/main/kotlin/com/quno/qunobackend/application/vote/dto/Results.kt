package com.quno.qunobackend.application.vote.dto

import com.quno.qunobackend.domain.vote.VoteTargetType

data class VoteResult(
    val targetType: VoteTargetType,
    val targetId: Long,
    val value: Int,
)
