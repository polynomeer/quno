package com.quno.qunobackend.application.answer.usecase

import com.quno.qunobackend.application.answer.dto.AnswerResult
import com.quno.qunobackend.application.answer.dto.WriteAnswerCommand
import com.quno.qunobackend.domain.answer.Answer
import com.quno.qunobackend.domain.answer.AnswerRepository
import com.quno.qunobackend.domain.common.OutboxEvent
import com.quno.qunobackend.domain.common.OutboxEventRepository
import com.quno.qunobackend.domain.common.OutboxEventTypes
import com.quno.qunobackend.domain.question.QuestionNotFoundException
import com.quno.qunobackend.domain.question.QuestionRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class WriteAnswerUseCase(
    private val questionRepository: QuestionRepository,
    private val answerRepository: AnswerRepository,
    private val outboxEventRepository: OutboxEventRepository,
) {
    @Transactional
    fun execute(command: WriteAnswerCommand): AnswerResult {
        questionRepository.findById(command.questionId) ?: throw QuestionNotFoundException(command.questionId)

        val saved = answerRepository.save(
            Answer.write(questionId = command.questionId, authorId = command.authorId, bodyMarkdown = command.body),
        )

        outboxEventRepository.save(
            OutboxEvent.create(
                eventType = OutboxEventTypes.NEW_ANSWER,
                aggregateType = "QUESTION",
                aggregateId = command.questionId,
                payload = """{"answerId":${saved.id}}""",
            ),
        )

        return saved.toResult()
    }
}

internal fun Answer.toResult(): AnswerResult = AnswerResult(
    id = requireNotNull(id),
    questionId = questionId,
    authorId = authorId,
    body = bodyMarkdown,
    isAccepted = isAccepted,
    createdAt = createdAt,
    updatedAt = updatedAt,
)
