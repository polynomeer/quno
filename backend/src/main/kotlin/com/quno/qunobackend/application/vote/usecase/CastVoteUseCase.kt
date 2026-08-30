package com.quno.qunobackend.application.vote.usecase

import com.quno.qunobackend.application.vote.dto.CastVoteCommand
import com.quno.qunobackend.domain.answer.AnswerNotFoundException
import com.quno.qunobackend.domain.answer.AnswerRepository
import com.quno.qunobackend.domain.question.QuestionNotFoundException
import com.quno.qunobackend.domain.question.QuestionRepository
import com.quno.qunobackend.domain.vote.SelfVoteException
import com.quno.qunobackend.domain.vote.Vote
import com.quno.qunobackend.domain.vote.VoteRepository
import com.quno.qunobackend.domain.vote.VoteTargetType
import org.springframework.stereotype.Service

@Service
class CastVoteUseCase(
    private val questionRepository: QuestionRepository,
    private val answerRepository: AnswerRepository,
    private val voteRepository: VoteRepository,
) {
    fun execute(command: CastVoteCommand) {
        val authorId = authorIdOf(command.targetType, command.targetId)
        if (authorId == command.voterId) throw SelfVoteException(command.targetType, command.targetId)

        val vote = Vote.cast(command.voterId, command.targetType, command.targetId, command.value)
        voteRepository.save(vote)
    }

    private fun authorIdOf(targetType: VoteTargetType, targetId: Long): Long = when (targetType) {
        VoteTargetType.QUESTION ->
            (questionRepository.findById(targetId) ?: throw QuestionNotFoundException(targetId)).authorId
        VoteTargetType.ANSWER ->
            (answerRepository.findById(targetId) ?: throw AnswerNotFoundException(targetId)).authorId
    }
}
