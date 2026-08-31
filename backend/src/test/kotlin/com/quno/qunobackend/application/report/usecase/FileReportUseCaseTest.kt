package com.quno.qunobackend.application.report.usecase

import com.quno.qunobackend.application.answer.usecase.InMemoryAnswerRepository
import com.quno.qunobackend.application.question.dto.CreateQuestionCommand
import com.quno.qunobackend.application.question.usecase.CreateQuestionUseCase
import com.quno.qunobackend.application.question.usecase.InMemoryQuestionRepository
import com.quno.qunobackend.application.question.usecase.InMemoryQuestionVersionRepository
import com.quno.qunobackend.application.report.dto.FileReportCommand
import com.quno.qunobackend.application.tag.usecase.InMemoryQuestionTagRepository
import com.quno.qunobackend.application.tag.usecase.InMemoryTagRepository
import com.quno.qunobackend.domain.answer.Answer
import com.quno.qunobackend.domain.answer.AnswerNotFoundException
import com.quno.qunobackend.domain.question.QuestionNotFoundException
import com.quno.qunobackend.domain.report.ReportReason
import com.quno.qunobackend.domain.report.ReportStatus
import com.quno.qunobackend.domain.report.ReportTargetType
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class FileReportUseCaseTest {
    private val questionRepository = InMemoryQuestionRepository()
    private val tagRepository = InMemoryTagRepository()
    private val createQuestionUseCase = CreateQuestionUseCase(
        questionRepository, InMemoryQuestionVersionRepository(), tagRepository, InMemoryQuestionTagRepository(tagRepository),
    )
    private val answerRepository = InMemoryAnswerRepository()
    private val reportRepository = InMemoryReportRepository()
    private val fileReportUseCase = FileReportUseCase(questionRepository, answerRepository, reportRepository)

    private fun aQuestion(): Long = createQuestionUseCase.execute(
        CreateQuestionCommand(authorId = 1L, title = "t", body = "body", environment = null, logs = null),
    ).id

    @Test
    fun `files a report against an existing question`() {
        val questionId = aQuestion()

        val result = fileReportUseCase.execute(
            FileReportCommand(reporterId = 2L, ReportTargetType.QUESTION, questionId, ReportReason.SPAM, "looks like spam"),
        )

        assertEquals(ReportStatus.PENDING, result.status)
        assertEquals(ReportReason.SPAM, result.reason)
    }

    @Test
    fun `files a report against an existing answer`() {
        val questionId = aQuestion()
        val answerId = requireNotNull(answerRepository.save(Answer.write(questionId, authorId = 2L, "an answer", 1)).id)

        val result = fileReportUseCase.execute(
            FileReportCommand(reporterId = 3L, ReportTargetType.ANSWER, answerId, ReportReason.LOW_QUALITY, null),
        )

        assertEquals(ReportTargetType.ANSWER, result.targetType)
        assertEquals(answerId, result.targetId)
    }

    @Test
    fun `multiple reports on the same target are all kept, not merged`() {
        val questionId = aQuestion()

        fileReportUseCase.execute(FileReportCommand(2L, ReportTargetType.QUESTION, questionId, ReportReason.SPAM, null))
        fileReportUseCase.execute(FileReportCommand(3L, ReportTargetType.QUESTION, questionId, ReportReason.DUPLICATE, null))

        assertEquals(2L, reportRepository.countByTarget(ReportTargetType.QUESTION, questionId))
    }

    @Test
    fun `rejects reporting a question that does not exist`() {
        assertFailsWith<QuestionNotFoundException> {
            fileReportUseCase.execute(FileReportCommand(2L, ReportTargetType.QUESTION, 999L, ReportReason.SPAM, null))
        }
    }

    @Test
    fun `rejects reporting an answer that does not exist`() {
        assertFailsWith<AnswerNotFoundException> {
            fileReportUseCase.execute(FileReportCommand(2L, ReportTargetType.ANSWER, 999L, ReportReason.SPAM, null))
        }
    }
}
