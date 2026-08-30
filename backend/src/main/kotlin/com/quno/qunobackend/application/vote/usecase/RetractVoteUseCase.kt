package com.quno.qunobackend.application.vote.usecase

import com.quno.qunobackend.domain.vote.VoteRepository
import com.quno.qunobackend.domain.vote.VoteTargetType
import org.springframework.stereotype.Service

@Service
class RetractVoteUseCase(
    private val voteRepository: VoteRepository,
) {
    fun execute(voterId: Long, targetType: VoteTargetType, targetId: Long) {
        voteRepository.delete(voterId, targetType, targetId)
    }
}
