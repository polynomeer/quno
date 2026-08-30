package com.quno.qunobackend.application.vote.dto

import com.quno.qunobackend.domain.vote.VoteTargetType

data class CastVoteCommand(
    val voterId: Long,
    val targetType: VoteTargetType,
    val targetId: Long,
    val value: Int,
)
