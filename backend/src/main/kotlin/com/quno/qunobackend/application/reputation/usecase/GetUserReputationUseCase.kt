package com.quno.qunobackend.application.reputation.usecase

import com.quno.qunobackend.domain.reputation.ReputationRepository
import com.quno.qunobackend.domain.reputation.UserReputation
import com.quno.qunobackend.domain.user.UserNotFoundException
import com.quno.qunobackend.domain.user.UserRepository
import org.springframework.stereotype.Service

/** Read-only reporting model — see ADR-0010, [UserReputation] is reused as-is through to the API layer. */
@Service
class GetUserReputationUseCase(
    private val userRepository: UserRepository,
    private val reputationRepository: ReputationRepository,
) {
    fun execute(userId: Long): UserReputation {
        userRepository.findById(userId) ?: throw UserNotFoundException(userId)
        return reputationRepository.compute(userId)
    }
}
