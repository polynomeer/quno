package com.quno.qunobackend.application.cluster.usecase

import com.quno.qunobackend.application.answer.dto.AcceptAnswerCommand
import com.quno.qunobackend.application.answer.dto.WriteAnswerCommand
import com.quno.qunobackend.application.answer.usecase.AcceptAnswerUseCase
import com.quno.qunobackend.application.answer.usecase.InMemoryAnswerRepository
import com.quno.qunobackend.application.answer.usecase.WriteAnswerUseCase
import com.quno.qunobackend.application.common.AnswerResultAssembler
import com.quno.qunobackend.application.common.InMemoryOutboxEventRepository
import com.quno.qunobackend.application.common.QuestionSummaryHydrator
import com.quno.qunobackend.application.question.dto.CreateQuestionCommand
import com.quno.qunobackend.application.question.usecase.CreateQuestionUseCase
import com.quno.qunobackend.application.question.usecase.InMemoryQuestionRepository
import com.quno.qunobackend.application.question.usecase.InMemoryQuestionVersionRepository
import com.quno.qunobackend.application.tag.usecase.InMemoryQuestionTagRepository
import com.quno.qunobackend.application.tag.usecase.InMemoryTagRepository
import com.quno.qunobackend.application.vote.usecase.InMemoryVoteRepository
import com.quno.qunobackend.domain.answer.AnswerNotFoundException
import com.quno.qunobackend.domain.cluster.AnswerNotAcceptedException
import com.quno.qunobackend.domain.cluster.AnswerNotInClusterException
import com.quno.qunobackend.domain.cluster.ClusterNotFoundException
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import org.junit.jupiter.api.Test

class DesignateSuperAnswerUseCaseTest {
    private val questionRepository = InMemoryQuestionRepository()
    private val questionVersionRepository = InMemoryQuestionVersionRepository()
    private val tagRepository = InMemoryTagRepository()
    private val questionTagRepository = InMemoryQuestionTagRepository(tagRepository)
    private val answerRepository = InMemoryAnswerRepository()
    private val questionClusterRepository = InMemoryQuestionClusterRepository()
    private val outboxEventRepository = InMemoryOutboxEventRepository()

    private val createQuestionUseCase = CreateQuestionUseCase(
        questionRepository, questionVersionRepository, tagRepository, questionTagRepository,
    )
    private val writeAnswerUseCase = WriteAnswerUseCase(
        questionRepository, questionVersionRepository, answerRepository, outboxEventRepository,
        AnswerResultAssembler(questionRepository, questionVersionRepository, InMemoryVoteRepository()),
    )
    private val acceptAnswerUseCase = AcceptAnswerUseCase(questionRepository, answerRepository, outboxEventRepository)
    private val markQuestionsAsSameProblemUseCase = MarkQuestionsAsSameProblemUseCase(questionRepository, questionClusterRepository)
    private val getClusterUseCase = GetClusterUseCase(
        questionClusterRepository, questionRepository, QuestionSummaryHydrator(questionRepository, questionTagRepository, InMemoryVoteRepository()),
    )
    private val useCase = DesignateSuperAnswerUseCase(questionClusterRepository, questionRepository, answerRepository, getClusterUseCase)

    private fun questionAskedBy(authorId: Long): Long =
        createQuestionUseCase.execute(
            CreateQuestionCommand(authorId = authorId, title = "t", body = "body", environment = null, logs = null),
        ).id

    @Test
    fun `designates an accepted answer from a cluster member as the Super Answer`() {
        val a = questionAskedBy(1L)
        val b = questionAskedBy(2L)
        val marked = markQuestionsAsSameProblemUseCase.execute(a, b)
        val answer = writeAnswerUseCase.execute(WriteAnswerCommand(questionId = a, authorId = 3L, body = "fix"))
        acceptAnswerUseCase.execute(AcceptAnswerCommand(answerId = answer.id, actorId = 1L))

        val result = useCase.execute(marked.clusterId, answer.id)

        assertEquals(answer.id, result.representativeAnswerId)
    }

    @Test
    fun `rejects an answer that has not been accepted`() {
        val a = questionAskedBy(1L)
        val b = questionAskedBy(2L)
        val marked = markQuestionsAsSameProblemUseCase.execute(a, b)
        val answer = writeAnswerUseCase.execute(WriteAnswerCommand(questionId = a, authorId = 3L, body = "fix"))

        assertFailsWith<AnswerNotAcceptedException> { useCase.execute(marked.clusterId, answer.id) }
    }

    @Test
    fun `rejects an answer that belongs to a question outside the cluster`() {
        val a = questionAskedBy(1L)
        val b = questionAskedBy(2L)
        val outsider = questionAskedBy(3L)
        val marked = markQuestionsAsSameProblemUseCase.execute(a, b)
        val answer = writeAnswerUseCase.execute(WriteAnswerCommand(questionId = outsider, authorId = 4L, body = "fix"))
        acceptAnswerUseCase.execute(AcceptAnswerCommand(answerId = answer.id, actorId = 3L))

        assertFailsWith<AnswerNotInClusterException> { useCase.execute(marked.clusterId, answer.id) }
    }

    @Test
    fun `rejects a cluster id that does not exist`() {
        assertFailsWith<ClusterNotFoundException> { useCase.execute(999L, 1L) }
    }

    @Test
    fun `rejects an answer id that does not exist`() {
        val a = questionAskedBy(1L)
        val b = questionAskedBy(2L)
        val marked = markQuestionsAsSameProblemUseCase.execute(a, b)

        assertFailsWith<AnswerNotFoundException> { useCase.execute(marked.clusterId, 999L) }
    }
}
