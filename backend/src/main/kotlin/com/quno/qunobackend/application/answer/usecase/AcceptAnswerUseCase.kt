package com.quno.qunobackend.application.answer.usecase

import com.quno.qunobackend.application.answer.dto.AcceptAnswerCommand
import com.quno.qunobackend.application.answer.dto.AcceptAnswerResult
import com.quno.qunobackend.domain.answer.AnswerNotFoundException
import com.quno.qunobackend.domain.answer.AnswerRepository
import com.quno.qunobackend.domain.question.QuestionAccessDeniedException
import com.quno.qunobackend.domain.question.QuestionNotFoundException
import com.quno.qunobackend.domain.question.QuestionRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * Accepting an answer touches two aggregates (Answer, Question) in one transaction.
 * The question row is locked (findByIdForUpdate) so a concurrent accept can't leave two
 * answers marked accepted for the same question — see docs/architecture/domain-model.md
 * "Accept invariant".
 */
@Service
class AcceptAnswerUseCase(
    private val questionRepository: QuestionRepository,
    private val answerRepository: AnswerRepository,
) {
    @Transactional
    fun execute(command: AcceptAnswerCommand): AcceptAnswerResult {
        val answer = answerRepository.findById(command.answerId) ?: throw AnswerNotFoundException(command.answerId)
        val question = questionRepository.findByIdForUpdate(answer.questionId)
            ?: throw QuestionNotFoundException(answer.questionId)
        if (question.authorId != command.actorId) throw QuestionAccessDeniedException(requireNotNull(question.id))

        answerRepository.findAcceptedByQuestionId(answer.questionId)
            ?.takeIf { it.id != answer.id }
            ?.let { answerRepository.save(it.unaccept()) }

        val acceptedAnswer = answerRepository.save(answer.accept())
        val resolvedQuestion = questionRepository.save(
            question.resolve(acceptedAnswerId = requireNotNull(acceptedAnswer.id)),
        )

        return AcceptAnswerResult(
            questionId = requireNotNull(resolvedQuestion.id),
            answerId = requireNotNull(acceptedAnswer.id),
            questionStatus = resolvedQuestion.status,
        )
    }
}
