package com.quno.qunobackend.application.vote.usecase

import com.quno.qunobackend.application.vote.dto.VoteResult
import com.quno.qunobackend.domain.vote.VoteRepository
import org.springframework.stereotype.Service

@Service
class ListMyVotesUseCase(
    private val voteRepository: VoteRepository,
) {
    fun execute(voterId: Long): List<VoteResult> =
        voteRepository.findByVoter(voterId).map { VoteResult(it.targetType, it.targetId, it.value) }
}
