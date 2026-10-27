package com.quno.qunobackend.application.answer.usecase

import com.quno.qunobackend.domain.stack.AnswerApplicabilityRepository
import com.quno.qunobackend.domain.stack.ApplicabilityRange

class InMemoryAnswerApplicabilityRepository : AnswerApplicabilityRepository {
    private data class Row(val answerId: Long, val questionId: Long, val versionNumber: Int, val range: ApplicabilityRange)

    private val rows = mutableListOf<Row>()

    fun versionNumberOf(answerId: Long): Int? = rows.firstOrNull { it.answerId == answerId }?.versionNumber

    override fun replaceForAnswer(answerId: Long, questionId: Long, versionNumber: Int, ranges: List<ApplicabilityRange>) {
        rows.removeAll { it.answerId == answerId }
        ranges.forEach { rows += Row(answerId, questionId, versionNumber, it) }
    }

    override fun findByAnswerIds(answerIds: Collection<Long>): Map<Long, List<ApplicabilityRange>> = rows.filter { it.answerId in answerIds }.groupBy({ it.answerId }, { it.range })

    override fun findByQuestionIds(questionIds: Collection<Long>): Map<Long, List<ApplicabilityRange>> = rows.filter { it.questionId in questionIds }.groupBy({ it.questionId }, { it.range })
}
