package com.quno.qunobackend.application.answer.usecase

import com.quno.qunobackend.domain.answer.Answer
import com.quno.qunobackend.domain.answer.AnswerRepository

class InMemoryAnswerRepository : AnswerRepository {
    private val byId = mutableMapOf<Long, Answer>()
    private var nextId = 1L

    override fun save(answer: Answer): Answer {
        val saved = if (answer.id == null) {
            Answer.reconstitute(
                id = nextId++,
                questionId = answer.questionId,
                authorId = answer.authorId,
                bodyMarkdown = answer.bodyMarkdown,
                isAccepted = answer.isAccepted,
                deletedAt = answer.deletedAt,
                createdAt = answer.createdAt,
                updatedAt = answer.updatedAt,
            )
        } else {
            answer
        }
        byId[requireNotNull(saved.id)] = saved
        return saved
    }

    override fun findById(id: Long): Answer? = byId[id]?.takeIf { it.deletedAt == null }

    override fun findAllByQuestionId(questionId: Long): List<Answer> =
        byId.values.filter { it.questionId == questionId && it.deletedAt == null }

    override fun findAcceptedByQuestionId(questionId: Long): Answer? =
        byId.values.find { it.questionId == questionId && it.isAccepted && it.deletedAt == null }
}
