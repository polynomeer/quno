package com.quno.qunobackend.application.answer.usecase

import com.quno.qunobackend.application.answer.dto.AnswerResult
import com.quno.qunobackend.application.answer.dto.WriteAnswerCommand
import com.quno.qunobackend.application.common.AnswerResultAssembler
import com.quno.qunobackend.domain.answer.Answer
import com.quno.qunobackend.domain.answer.AnswerRepository
import com.quno.qunobackend.domain.common.OutboxEvent
import com.quno.qunobackend.domain.common.OutboxEventRepository
import com.quno.qunobackend.domain.common.OutboxEventTypes
import com.quno.qunobackend.domain.question.QuestionNotFoundException
import com.quno.qunobackend.domain.question.QuestionRepository
import com.quno.qunobackend.domain.question.QuestionVersionRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class WriteAnswerUseCase(
    private val questionRepository: QuestionRepository,
    private val questionVersionRepository: QuestionVersionRepository,
    private val answerRepository: AnswerRepository,
    private val outboxEventRepository: OutboxEventRepository,
    private val answerResultAssembler: AnswerResultAssembler,
) {
    @Transactional
    fun execute(command: WriteAnswerCommand): AnswerResult {
        val question = questionRepository.findById(command.questionId)
            ?: throw QuestionNotFoundException(command.questionId)
        val targetVersionNumber = questionVersionRepository.findById(requireNotNull(question.latestVersionId)).let {
            requireNotNull(it).versionNumber
        }

        val saved = answerRepository.save(
            Answer.write(
                questionId = command.questionId,
                authorId = command.authorId,
                bodyMarkdown = command.body,
                targetVersionNumber = targetVersionNumber,
            ),
        )

        // questionAuthorId: the question's author is always notified, even if they never
        // explicitly watched their own question — see DispatchOutboxEventsUseCase's kdoc.
        outboxEventRepository.save(
            OutboxEvent.create(
                eventType = OutboxEventTypes.NEW_ANSWER,
                aggregateType = "QUESTION",
                aggregateId = command.questionId,
                payload = """{"answerId":${saved.id},"actorId":${command.authorId},"questionAuthorId":${question.authorId}}""",
            ),
        )

        return answerResultAssembler.toResult(saved)
    }
}
