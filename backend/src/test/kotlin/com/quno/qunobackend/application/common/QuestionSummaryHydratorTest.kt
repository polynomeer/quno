package com.quno.qunobackend.application.common

import com.quno.qunobackend.application.answer.usecase.InMemoryAnswerRepository
import com.quno.qunobackend.application.question.usecase.InMemoryQuestionRepository
import com.quno.qunobackend.application.question.usecase.InMemoryQuestionVersionRepository
import com.quno.qunobackend.application.tag.usecase.InMemoryQuestionTagRepository
import com.quno.qunobackend.application.tag.usecase.InMemoryTagRepository
import com.quno.qunobackend.application.vote.usecase.InMemoryVoteRepository
import com.quno.qunobackend.domain.answer.Answer
import com.quno.qunobackend.domain.question.Question
import com.quno.qunobackend.domain.question.QuestionVersion
import com.quno.qunobackend.domain.tag.Tag
import com.quno.qunobackend.domain.vote.Vote
import com.quno.qunobackend.domain.vote.VoteTargetType
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

/** Rewritten to batch its three lookups instead of querying per id (production-readiness.md
 * B-4, N+1 fix) — these tests lock in that the visible behavior (order, dropped-if-missing,
 * tags, score) didn't change. */
class QuestionSummaryHydratorTest {
    private val questionRepository = InMemoryQuestionRepository()
    private val tagRepository = InMemoryTagRepository()
    private val questionTagRepository = InMemoryQuestionTagRepository(tagRepository)
    private val voteRepository = InMemoryVoteRepository()
    private val answerRepository = InMemoryAnswerRepository()
    private val questionVersionRepository = InMemoryQuestionVersionRepository()
    private val hydrator = QuestionSummaryHydrator(questionRepository, questionTagRepository, voteRepository, answerRepository, questionVersionRepository)

    @Test
    fun `hydrates in the given id order, dropping ids that no longer resolve`() {
        val q1 = questionRepository.save(Question.open(1L, "First"))
        val q2 = questionRepository.save(Question.open(1L, "Second"))
        val tag = tagRepository.save(Tag.create("kotlin"))
        questionTagRepository.attach(requireNotNull(q1.id), requireNotNull(tag.id))
        voteRepository.save(Vote(voterId = 99L, targetType = VoteTargetType.QUESTION, targetId = requireNotNull(q2.id), value = 1))

        val missingId = 999L
        val result = hydrator.hydrate(listOf(requireNotNull(q2.id), missingId, requireNotNull(q1.id)))

        assertEquals(listOf(q2.id, q1.id), result.map { it.id })
        assertEquals(listOf("kotlin"), result.first { it.id == q1.id }.tags)
        assertEquals(1L, result.first { it.id == q2.id }.score)
        assertEquals(0L, result.first { it.id == q1.id }.score)
    }

    @Test
    fun `attaches answer count, accepted flag and latest version number`() {
        val question = questionRepository.save(Question.open(1L, "Revised"))
        val questionId = requireNotNull(question.id)
        listOf(1, 2, 3).forEach { number ->
            questionVersionRepository.save(QuestionVersion.create(questionId, number, "Revised", "body", null, null, 1L))
        }
        answerRepository.save(Answer.write(questionId, 2L, "first", targetVersionNumber = 1))
        val accepted = answerRepository.save(Answer.write(questionId, 3L, "second", targetVersionNumber = 3))
        questionRepository.save(question.resolve(requireNotNull(accepted.id)))

        val result = hydrator.hydrate(listOf(questionId)).single()

        assertEquals(2, result.answerCount)
        assertEquals(true, result.hasAcceptedAnswer)
        assertEquals(3, result.versionNumber)
    }

    @Test
    fun `returns empty for an empty id list without querying anything`() {
        assertEquals(emptyList(), hydrator.hydrate(emptyList()))
    }
}
