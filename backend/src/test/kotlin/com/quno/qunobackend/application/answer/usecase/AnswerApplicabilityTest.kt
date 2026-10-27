package com.quno.qunobackend.application.answer.usecase

import com.quno.qunobackend.application.answer.dto.ReviseAnswerCommand
import com.quno.qunobackend.application.answer.dto.WriteAnswerCommand
import com.quno.qunobackend.application.common.AnswerResultAssembler
import com.quno.qunobackend.application.common.InMemoryOutboxEventRepository
import com.quno.qunobackend.application.question.dto.CreateQuestionCommand
import com.quno.qunobackend.application.question.usecase.CreateQuestionUseCase
import com.quno.qunobackend.application.question.usecase.InMemoryQuestionRepository
import com.quno.qunobackend.application.question.usecase.InMemoryQuestionVersionRepository
import com.quno.qunobackend.application.tag.usecase.InMemoryQuestionTagRepository
import com.quno.qunobackend.application.tag.usecase.InMemoryTagRepository
import com.quno.qunobackend.application.vote.usecase.InMemoryVoteRepository
import com.quno.qunobackend.domain.stack.ApplicabilityRange
import com.quno.qunobackend.domain.stack.Ecosystem
import com.quno.qunobackend.domain.stack.InvalidApplicabilityRangeException
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

/** Phase 63.3 (ADR-0077): answer applicability ranges through write, revise and the assembler. */
class AnswerApplicabilityTest {
    private val questionRepository = InMemoryQuestionRepository()
    private val questionVersionRepository = InMemoryQuestionVersionRepository()
    private val tagRepository = InMemoryTagRepository()
    private val answerRepository = InMemoryAnswerRepository()
    private val answerVersionRepository = InMemoryAnswerVersionRepository()
    private val outboxEventRepository = InMemoryOutboxEventRepository()
    private val applicabilityRepository = InMemoryAnswerApplicabilityRepository()
    private val assembler = AnswerResultAssembler(questionRepository, questionVersionRepository, InMemoryVoteRepository(), applicabilityRepository)
    private val createQuestionUseCase = CreateQuestionUseCase(
        questionRepository,
        questionVersionRepository,
        tagRepository,
        InMemoryQuestionTagRepository(tagRepository),
    )
    private val writeAnswerUseCase = WriteAnswerUseCase(
        questionRepository,
        questionVersionRepository,
        answerRepository,
        answerVersionRepository,
        outboxEventRepository,
        assembler,
        applicabilityRepository,
    )
    private val reviseAnswerUseCase = ReviseAnswerUseCase(
        answerRepository,
        answerVersionRepository,
        questionRepository,
        outboxEventRepository,
        applicabilityRepository,
    )

    private val boot = ApplicabilityRange(Ecosystem.GRADLE_PLUGIN, "org.springframework.boot", "3.2.0", "4.0.0")
    private val react = ApplicabilityRange(Ecosystem.NPM, "react", "18.0.0", null)

    private fun aQuestion(): Long = createQuestionUseCase.execute(
        CreateQuestionCommand(authorId = 1L, title = "t", body = "body", environment = null, logs = null),
    ).id

    @Test
    fun `writing an answer stores its ranges and returns them`() {
        val result = writeAnswerUseCase.execute(WriteAnswerCommand(aQuestion(), 2L, "body", applicability = listOf(boot, react)))

        assertEquals(listOf(boot, react), result.applicability)
        assertEquals(1, applicabilityRepository.versionNumberOf(result.id))
    }

    @Test
    fun `revising without ranges keeps them, an empty list clears them`() {
        val answerId = writeAnswerUseCase.execute(WriteAnswerCommand(aQuestion(), 2L, "body", applicability = listOf(boot))).id

        reviseAnswerUseCase.execute(ReviseAnswerCommand(answerId, actorId = 2L, body = "v2"))
        assertEquals(listOf(boot), applicabilityRepository.findByAnswerIds(listOf(answerId))[answerId])

        reviseAnswerUseCase.execute(ReviseAnswerCommand(answerId, actorId = 2L, body = "v3", applicability = listOf(react)))
        assertEquals(listOf(react), applicabilityRepository.findByAnswerIds(listOf(answerId))[answerId])
        assertEquals(3, applicabilityRepository.versionNumberOf(answerId))

        reviseAnswerUseCase.execute(ReviseAnswerCommand(answerId, actorId = 2L, body = "v4", applicability = emptyList()))
        assertTrue(applicabilityRepository.findByAnswerIds(listOf(answerId)).isEmpty())
    }

    @Test
    fun `rejects two ranges for the same package and too many ranges`() {
        val questionId = aQuestion()
        val sameBoot = boot.copy(fromVersion = "3.0.0")
        assertFailsWith<InvalidApplicabilityRangeException> {
            writeAnswerUseCase.execute(WriteAnswerCommand(questionId, 2L, "body", applicability = listOf(boot, sameBoot)))
        }

        val tooMany = (1..ApplicabilityRange.MAX_PER_ANSWER + 1).map { ApplicabilityRange(Ecosystem.NPM, "pkg-$it", "1.0", null) }
        assertFailsWith<InvalidApplicabilityRangeException> {
            writeAnswerUseCase.execute(WriteAnswerCommand(questionId, 2L, "body", applicability = tooMany))
        }
        assertTrue(answerRepository.findAllByQuestionId(questionId).isEmpty(), "validation runs before anything is written")
    }
}
